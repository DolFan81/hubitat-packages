/**
 *  Device Temperature Check Child App (DTC Child App)
 *    by: Scott Wade
 *    created: 2025-03-08
 *
 *  Licensed Virtual the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Special thanks to @bravenel (Bruce) for his share of the best date/time compare routine. It Sure made my work easier!
 *  https://community.hubitat.com/t/time-range-that-crosses-midnight/99747/2?
 *  and 
 *  Special thanks to Jost Schwider for "borrowing" his html link code in Just Simple Battery Statistics!
 *  
 *  Change History:
 *
 *    Date         	Who           	What
 *    ----         	---           	----
 *    2025-03-09    s.wade			add quiet time modules/logic
 *    2025-03-12   	s.wade	    	code cleanup before release
 *    2025-03-31   	s.wade	    	improve scheduling code
 *    2025-04-03   	s.wade	    	added code to reschedule device checks after hub reboots
*/

static String getVersion()	{  return '1.0.3'  }
import groovy.time.TimeCategory

definition(
    name: "DTCchildApp",
    namespace: "myHubitat",      
    parent: "myHubitat:Device Temperature Check", // <-- must include! (adjust as needed)
    author: "Scott Wade",
    description: "Notify if Device Temperature not reported in X minutes",
    category: "Utility",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
   	importUrl: "https://raw.githubusercontent.com/DolFan81/hubitat-packages/refs/heads/main/DeviceTemperatureChecks/DTCchildApp.groovy"
)

preferences {
  page(name: "pageMain", install: true, uninstall: true) 
  {
    section("")
    {
        label title: "Customize installed child app name:", required: true
        input name: "TempDevices", type: "capability.temperatureMeasurement", title: "Temperature Devices", required: true, multiple: true
        input "IntervalCheckMinutes", "enum", title: "Check Values Every", required: true, defaultValue: "120", options: ["15", "20", "30", "60","90", "120", "180","240","360","720","1080","1440"]
        input name: "sendPushMessage", type: "capability.notification", title: "Notification Devices: Hubitat PhoneApp or Pushover", multiple: true, required: false, submitOnChange: true
       	if(sendPushMessage)
       	{
        	input "optionQuietTime", "bool", title: "Set a quiet time for notifications?", defaultValue: false, required: false, submitOnChange: true
	    	if(optionQuietTime)
   			{
   				input "startQuietTime", "Time", title: "Select Start Time?", defaultValue: false, required: true
   				input "endQuietTime", "Time", title: "Select End Time?", defaultValue: false, required: true
   			}
   			paragraph "<hr>"
        }
       	input "createLogEntry", "bool", title: "Create a log entry when check fails?", defaultValue: false, required: false, width:3, submitOnChange: true
       	if(createLogEntry)
   	   	{
       		input "logWarnError", "bool", title: "Select for log.Error (on) - log.Warning when (off)?", defaultValue: false, required: false, width:3
       	}
   		paragraph "<hr>"
        input "logOutput", "bool", title: "Enable App logging?", defaultValue: false, required: false,width:3
		input "debugOutput", "bool", title: "Enable Debug logging?", defaultValue: false, required: false,width:3
       	paragraph "<hr>"
    	
      
		//log.info app.getInstallationState()
		if (app.getInstallationState() == 'COMPLETE') {
			// Make sure the app has completed install (don't run thid the first time child app runs) 
			strEvents = getLastEvents()
       		paragraph "Last Reported Temperature Event<hr>" + strEvents + "<hr>"
    	}
        paragraph "<hr><div style='color:black;text-align:center'>version ${getVersion()}<br><small>Copyright \u00a9 2024-2025 &emsp;-&emsp; All rights reserved.</small><br></div>"
	}	
  }
}

def installed() {
	if(debugOutput) logMsg("*** Installed with settings: ${settings}")
	initialize()
}

def updated() {
    if(debugOutput) logMsg("*** Updated with settings: ${settings}")
    MainHandler()
    //reset the Instructions so not opened when loading page
    app.updateSetting("Instructions", [value:"false", type:"bool"])
}

def uninstalled() {
	if(debugOutput) logMsg("*** Uninstalled App")
    unschedule()
    unsubscribe()
}

def initialize() {
	if(debugOutput) logMsg("*** Initializing App")
    updated()
}

def hubRestartHandler(evt)
{
    log.info "${app.getName()} - v${getVersion()} initialized"
    NextScheludedRun()
}

void MainHandler()
{
    if(debugOutput) logMsg("*** MainHandler - Start")
    
    unsubscribe()
    //make sure to re-subscribe back to monitor for system starts.
    subscribe(location, "systemStart", hubRestartHandler)
    
    iDeviceCnt = DeviceCount()
    
    def newMap = [:]
    def iNum = 0
    
    TempDevices.each
    {
    	iNum += 1
        def devMap = [:]
        
        strName = it.getLabel()
        if(strName == null) { strName = it.getName() }
        
        if(debugOutput) logMsg("Name: " + strName)
        devMap = state."$strName"
        if(debugOutput) logMsg("MainHandler - Map: " + devMap)
        
        if(devMap == null)  // new device so create new entry
        {
            devMap = [:]  //needed to creat a new Map entry

            devMap.Name = strName
            if(debugOutput) logMsg("New strName: " + strName)
            if(debugOutput) logMsg("New Map - Name: " + strName)
        	if(debugOutput) logMsg("Name: " + devMap.Name)

            devMap.Temp = it.currentValue("temperature")
        	if(debugOutput) logMsg("Temp: " + devMap.Temp)
        
        	Date firstTime = new Date()
        	if(debugOutput) logMsg("Seed Date: " + firstTime)
            devMap.Time = firstTime.format("YYYY-MM-dd'T'HH:mm:ss.SSSZ", location.timeZone)
        	if(debugOutput) logMsg("Map Date: " + devMap.Time)

            if(debugOutput || logOutput) logMsg("Added New Device Map: $devMap")
            stateEntry(devMap.Name, devMap.Temp, devMap.Time)
        }
    	newMap[iNum] = devMap
		if(debugOutput) logMsg("devMap = $devMap")
            
	    //need to re-subscribe as well
        if(debugOutput) logMsg("Subscribing $strName for Temperature Events")
        subscribe(it, "temperature", eventHandler)
    }
       
    def savedSchedule = state.savedSchedule
    //def savedCreated = state.created
    
   	state.clear()  // clear the state of entries and then recreate them. Necessary to account for removed devices 
   	if(debugOutput) logMsg("iNum = ${iNum}")
   	for (int i = 1; i <=  iNum; i++) {
   		if(debugOutput) logMsg("reCreate Map entry: " + newMap[i])
       	stateEntry(newMap[i].Name, newMap[i].Temp, newMap[i].Time)
	}
   	state.savedSchedule = savedSchedule        
   	//state.created = savedCreated
        
    if(debugOutput) logMsg("Device Count: " + iDeviceCnt)
    if(iDeviceCnt == 0) //only run to schedule 1st time devices selected
    {
		unschedule()  // need to make sure it was not already scheduled
	    //scheldule next run
        if(debugOutput || logOutput) logMsg("Scheduling Job to run Checks")
    	NextScheludedRun()
    }
    
    if(debugOutput) logMsg("Saved Interval: " + state.savedSchedule) 
    if(debugOutput) logMsg("New Interval: " + IntervalCheckMinutes)

    //check if IntervalCheckMinutes changes, if so do a schedule update
    if(IntervalCheckMinutes.toString() == state.savedSchedule)
    {
		if(debugOutput) logMsg("Schedule Not changed so keep current Job")
    } else
    {
        if(debugOutput || logOutput) logMsg("Schedule changed so create new Job for running checks")
        NextScheludedRun()
    }
    //save below for testing
    //NextScheludedRun()
    //checkSchedule()
    
    if(debugOutput) logMsg("*** MainHandler - End")
}

def stateEntry(def sName, def sTemp, def sTime)
{
	if(debugOutput) logMsg("*** stateEntry - Start")

    devMap = [:]   //create new Map entry

    if(debugOutput) logMsg("Creating a new State.Entry")

    devMap.Name = sName
    if(debugOutput) logMsg("Name: " + devMap.Name)

    devMap.Temp = sTemp
   	if(debugOutput) logMsg("Name: " + devMap.Temp)
        
   	devMap.Time = sTime
   	if(debugOutput) logMsg("Map Date: " + devMap.Time)

    state."$sName" = devMap
  	
    if(debugOutput) logMsg("*** stateEntry - End")
}

void eventHandler(evt)
{
	if(debugOutput) logMsg("*** eventHandler - Start")

    String myMessage = " "
	myEvtTemp = evt.device.currentValue("temperature")
    if(debugOutput) logMsg("Event Temp: " + myEvtTemp)
	myEvtName = evt.displayName
    if(debugOutput) logMsg("Event Name: " + myEvtName)

	def devMap = [:]
        
    devMap.Temp = myEvtTemp
    if(debugOutput || logOutput) logMsg("Subscribed Event Occurred: $myEvtName is currently: " + devMap.Temp) 

    Date myEvtTime = new Date()
    //firstTime.format("YYYY-MM-dd'T'HH:mm:ss.SSSZ", location.timeZone)
    devMap.Time = myEvtTime.format("YYYY-MM-dd'T'HH:mm:ss.SSSZ", location.timeZone)
    if(debugOutput) logMsg("Event:Map Time: " + devMap.Time)
        
    devMap.Name = myEvtName
    if(debugOutput) logMsg("Event:Map Name: " + devMap.Name)

    state."$devMap.Name" = devMap
	if(debugOutput) logMsg("Event:Map: " + devMap.toString())
    
    if(debugOutput) logMsg("*** eventHandler - End")
}

void checkSchedule()
{
	if(debugOutput) logMsg("*** checkSchedule - Start")
    
    //check device schedule
   	Date myDate1 = new Date()
   	int scheduledMinutes = IntervalCheckMinutes as int
    use( TimeCategory ) {
 		myDate2 = myDate1 - scheduledMinutes.minutes
	}
	timeCheck1 = myDate1.format("YYYY-MM-dd'T'HH:mm:ss.SSSZ", location.timeZone)
	timeCheck2 = myDate2.format("YYYY-MM-dd'T'HH:mm:ss.SSSZ", location.timeZone)
        
    if(debugOutput) logMsg("timeCheck2: " + timeCheck2)
    if(debugOutput) logMsg("timeCheck1: " + timeCheck1)
    
    TempDevices.each
    {
        def devMap = [:]
        
        strName = it.getLabel()
        if(strName == null) { strName = it.getName() }
        if(debugOutput) logMsg("Name: " + strName)
        devMap = state."$strName"
        if(debugOutput) logMsg("Map: " + devMap)
        
        if(devMap != null)
        {
            if(debugOutput) logMsg("$strName Map: " + state."$devMap.Name")
            if(debugOutput) logMsg("Name: " + devMap.Name)
            if(debugOutput) logMsg("Temp: " + devMap.Temp)
            if(debugOutput) logMsg("Time: " + devMap.Time)
            
	        String myMessage = " "
            compareTo = devMap.Time
            if(debugOutput) logMsg("before compareTo: " + compareTo)
            if(debugOutput) logMsg("timeCheck1: " + timeCheck1)
            if(debugOutput) logMsg("timeCheck2: " + timeCheck2)
            if(debugOutput || logOutput) logMsg("Scheduled check for $strName")
            
            if (CompareTimes(timeCheck2, timeCheck1, compareTo))
    	    {	
    			myMessage =  "Passed - Temperature Time Check for: " + strName
        		if (sendPushMessage)
        		{
	        		//sendNotification(myMessage)
        		}
                if(debugOutput || logOutput) logMsg(myMessage)
        	}
        	else
        	{
                myMessage =  "Failed - Temperature Time Check for: " + strName
                if(createLogEntry)
                {
                	if(logWarnError)
                    {
                    	log.error "$strName Failed Temperature Check"
                    }
                    else
                    	{
                    		log.warn "$strName Failed Temperature Check"
                        }
                   if(debugOutput || logOutput) logMsg("Log entry created for - " + myMessage) 
                }
        		if (sendPushMessage)
        		{
                	if(!optionQuietTime)
                	{
                		sendNotification(myMessage)
                        if(debugOutput || logOutput) logMsg("Failed $strName message sent")
                	}
                	else
                	{
			        	//QuietTime compare to see if we will send message
                        if(debugOutput) logMsg("Start Quiet Time: " + startQuietTime)
    					if(debugOutput) logMsg("End Quiet Time: " + endQuietTime)
                        
                        if (!CompareTimes(startQuietTime, endQuietTime))
                        {
                            sendNotification(myMessage)
                            if(debugOutput || logOutput) logMsg("Failed $strName message sent [Not Quite Time]")

                        }
                        else
                        {
                            if(debugOutput || logOutput) logMsg("Failed $strName message Not sent [Quite Time enforced]")
                        }
                	}
        		}
                if(debugOutput || logOutput) logMsg(myMessage)
        	}
        
        } else
        {
            if(debugOutput) logMsg("No Event Maps")
        }
    }
    //scheldule next run
    NextScheludedRun()
    
    if(debugOutput) logMsg("*** checkSchedule - End")
}

def NextScheludedRun() {

    if(debugOutput) logMsg("*** NextScheludedRun - Start")
    
    def currDateTime = new Date() 
    int intCheckMinutes = IntervalCheckMinutes as int
    if(debugOutput) logMsg("Minutes to Increase: " + intCheckMinutes)

    use (TimeCategory)
    {
        NextSchDateTime = currDateTime + intCheckMinutes.minutes
    }
    if(debugOutput) logMsg("Next Scheduled Time" + NextSchDateTime)
    
    // break out parts for cron expression in schedule command 
    def srhours = NextSchDateTime.hours
    def srminutes = NextSchDateTime.minutes
    def srday = NextSchDateTime.date
    def srmonth = NextSchDateTime.format('MM') as int
    if(debugOutput) logMsg("Next Scheduled Check hours: " + srhours)
    if(debugOutput) logMsg("Next Scheduled Check minutes: " + srminutes)
    if(debugOutput) logMsg("Next Scheduled Check day: " + srday)
    if(debugOutput) logMsg("Next Scheduled Check month: " + srmonth)
    
    schedule("00 $srminutes $srhours $srday $srmonth ? *", checkSchedule)
    
    state.savedSchedule = IntervalCheckMinutes
    if(debugOutput) logMsg("Saved Interval: " + state.savedSchedule)
    
    def elapsedMilliSeconds = NextSchDateTime.getTime() - currDateTime.getTime()
    int sMinutes = elapsedMilliSeconds/60000
    int sMinuteDiff = sMinutes - intCheckMinutes
    
    if(sMinuteDiff != 0)
    {
		if(debugOutput) logMsg("Time Diff: " + sMinuteDiff)
    }
    else
    {
		if(debugOutput) logMsg("No Time Diff: " + sMinuteDiff)
    }
    
    if(debugOutput) logMsg("*** NextScheludedRun - End")
}

def sendNotification(String sMessage)
{
	if(debugOutput) logMsg("*** sendNotification - Start")
    
    sendPushMessage.deviceNotification(sMessage)    
    
    if(debugOutput) logMsg("*** sendNotification - End")
}

def getLastEvents()
{
	if(debugOutput) logMsg("*** getLastEvents - Start")
    
    def myDevStrings = ""
    int iCount = 0
    myDevStrings = "<table>"  // style="font-size:90%"

    TempDevices.each
    {
        //Map devMap
		strName = it.getLabel()
        if(strName == null) { strName = it.getName() }
        if(debugOutput) logMsg("Name: " + strName)
        
        savedValue = state."$strName"
        if(debugOutput) logMsg("Saved: " + savedValue)
        devMap = state."$strName"   //savedValue
        if(debugOutput) logMsg("Map: " + devMap)
        
        def cnvTime = ChangeDateTime(devMap.Time)
        def strNice = "[$devMap.Name, $devMap.Temp, $cnvTime]"
		
        if(strNice != null)
        {
        	iCount += 1
            htmlName = devMap.Name //strNice.toString()
    		htmlLink = "<a href='/device/edit/$it.id' target=_blank>$htmlName</a>"
            if(debugOutput) logMsg(htmlLink)

            myDevStrings += "<tr>" 
            myDevStrings += "<td>$htmlLink </td>"
		    myDevStrings += "<td>&emsp;$devMap.Temp </td>"
    		myDevStrings += "<td>&emsp;$cnvTime </td>"
	    	myDevStrings += "</tr>" 
        }
    }
    
    if(iCount == 0)
	{
	   	myDevStrings += "<tr>" 
        myDevStrings += "<td> No Event Maps</td>"
	   	myDevStrings += "</tr>" 
    }
    
    myDevStrings += "</table>" 
    
    if(debugOutput) logMsg("*** getLastEvents - End")

    return myDevStrings
}

def DeviceCount()
{
	if(debugOutput) logMsg("*** DeviceCount - Start")
    
    int iDeviceCnt = 0
    TempDevices.each
    {
		strName = it.getLabel()
        if(strName == null) { strName = it.getName() }
        savedValue = state."$strName"
        if(debugOutput) logMsg("Counting Running Devices: " + savedValue)
        
        if(savedValue != null) {iDeviceCnt += 1}
    }
    if(debugOutput) logMsg("Running Device Count: " + iDeviceCnt)
    
    if(debugOutput) logMsg("*** DeviceCount - End")
    
    return iDeviceCnt    
}

def ChangeDateTime(inputDateTime) {
    
	if(debugOutput) logMsg("*** ChangeDateTime - Start")
    
    //from .format("YYYY-MM-dd'T'HH:mm:ss.SSSZ)
    int offsetIndex=inputDateTime.indexOf("T")
    if(debugOutput) logMsg("Change DateTime" + inputDateTime)
    if(debugOutput) logMsg("Change DateTime" + offsetIndex)
    def partDate = inputDateTime.substring(0,offsetIndex)
	if(debugOutput) logMsg("Change DateTime" + partDate)
    def partTime = inputDateTime.substring(offsetIndex+1, offsetIndex + 6)
	if(debugOutput) logMsg("Change DateTime" + partTime)
	
    if(debugOutput) logMsg("Dates: " + partDate)
	if(debugOutput) logMsg("Time: " + partTime)
    
    if(debugOutput) logMsg("*** ChangeDateTime - End")
    
    return (partDate + "&emsp;" + partTime)
}

def CompareTimes(starting, ending, dTime = null) {

	if(debugOutput) logMsg("*** CompareTimes - Start")
    
    if(debugOutput) logMsg("dTime: " + dTime)
    if(debugOutput) logMsg("starting: " + starting)
    if(debugOutput) logMsg("ending: " + ending)
    
    if(dTime == null)
   	{
    	Date dt = new Date()
        if(debugOutput) logMsg("dt: " + dt)
    	cTime = dt.format("YYYY-MM-dd'T'HH:mm:ss.SSSZ", location.timeZone)
        if(debugOutput) logMsg("cTime: " + cTime)
    	currTime = timeToday(cTime, location.timeZone).time
    	if(debugOutput) logMsg("currTime: " + currTime)
    }
    else
    {
        currTime = timeToday(dTime, location.timeZone).time
	    if(debugOutput) logMsg("Parm-currTime: " + currTime)
    }
   
    //keep this below for time testing
    //currTime = timeToday("2025-03-11T04:00:00.000-0400", location.timeZone).time
    //log.info "New Compare: " + currTime
    
	start = timeToday(starting, location.timeZone).time
    if(debugOutput) logMsg("start: $starting -> " + start)
    
	stop = timeToday(ending, location.timeZone).time
    if(debugOutput) logMsg("end: $ending -> " + stop)
	
    if(debugOutput) logMsg("compare to: " + currTime)
    if(debugOutput) logMsg("start: " + start)
    if(debugOutput) logMsg("stop: " + stop)
    
    if(debugOutput) logMsg("*** CompareTimes - End")
    
    return start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
}

def logMsg(msg) 
{
	if(debugOutput)
    {
    	log.debug msg
    }
    else
    {
        log.info msg
    }
}
