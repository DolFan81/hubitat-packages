/**
 *  Device Temperature Check Parent App
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
 *  Change History:
 *
 *    Date         	Who           	What
 *    ----         	---           	----
 *    2025-03-08    s.wade			create parent and child framework
 *    2025-04-03    s.wade			added importURL to App code
*/

static String getVersion()	{  return '1.0.4'  }
definition(
    name: "Device Temperature Check",
    namespace: "myHubitat",
    author: "Scott Wade",
    description: "Notify if Device Temperature not reported in X minutes",
    singleInstance: true,
    installOnOpen: true,
    category: "Utility",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
   	importUrl: "https://raw.githubusercontent.com/DolFan81/hubitat-packages/refs/heads/main/DeviceTemperatureChecks/DeviceTemperatureCheck.groovy"
)

preferences {
  page(name: "mainAppPage", install: true, uninstall: true) 
  {
    //log.info app.getInstallationState()
    if (app.getInstallationState() == 'INCOMPLETE') {
    	// Make sure the app has completed install (first run through)
        section() {
        	paragraph('Please press "Done" to finish installing this app, then re-open it to add Deice Groups child instances.')
    	}
    }
	else
    {
		section("")
    	{
	    	app name: "DTCchildApp", appName: "DTCchildApp", namespace: "myHubitat", title: "Add New Group of Devices", multiple: true
    	}
    	section("")
        {
	    	input "Instructions", "bool", title: "Show app Instructions", defaultValue: false, required: false, submitOnChange: true
   			if(Instructions == true)
			{
        		strInstrucions = getInstructions()
       			paragraph "<hr>App Instructions<hr>" + strInstrucions + "<hr>"
        	}
       	}
	}
	section("")
   	{
		paragraph "<hr><div style='color:black;text-align:center'>version ${getVersion()}<br><small>Copyright \u00a9 2024-2025 &emsp;-&emsp; All rights reserved.</small><br></div>" 	         	
    }
  }
}

def installed() {
	logMsg("Installed Device Temperature Check App")
	initialize()
}

def updated() {
}

def uninstalled() {
	logMsg("Uninstalled Device Temperature CheckApp")
    // unsubscribe to all events
    unschedule()
    unsubscribe()
}

def initialize() {
    updated()
}

def getInstructions()
{
	def myDevStrings = "" 
    
    myDevStrings += '<table style="background-color:#F8F8FF;">'
    myDevStrings += '<tr><td>'
	myDevStrings += '<table cellpadding="3" cellspacing="1" style="background-color:#FFFFE0;">'
    myDevStrings += '<h2 style="color: red;">Preamble…</h2>'
    myDevStrings += '<p style="font-size:18px; letter-spacing: 1px; line-height: 1.4;">I use this app for peace of mind! Knowing that devices I have spread throughout my house to report temperatures are indeed providing temperature updates. In my experience, how often this happens differs between manufactures, line of products, and settings. It has taken me a little bit of work to get the right devices and settings working for the reporting frequency I am hoping for. For my refrigerators and freezers temperature sensors, I like to keep a closer eye on them, so how often I check them is greater than the ones I have in each room that I use to control my keen vents. This being the reason for the parent/child app format I used here. Figure out the frequency you hope for, add the devices to that group, and finally decide how you want the results.</p>'
    myDevStrings += '<p style="font-size:18px; letter-spacing: 1px; line-height: 1.4;">What I have noticed in my monitoring of device temperatures, is once I get a group of devices and settings reporting without issues, I have seen what works for days at a time will on a rare occasion have one device miss a temperature check. Since I have selected to get a Pushover notification, I wait until the device reports another issue, or maybe 2 missed temperatures, before I go check the devices information page/events or even the physical device itself to resolve the issue. You may have a setup that works perfectly and not see these occasional issues, but if you are more like me, you will at least be informed a device has missed a reporting time, or has stopped reporting at all with the numerous notifications and/or log entries that you have selected to get.'
    myDevStrings += '<p style="font-size:18px; letter-spacing: 1px; line-height: 1.4;">I have on occasion while being away from home and being notified more than once a device has missed temperature reporting, but after getting home and checking on the issue it has been resolved. What I am saying is this App, if you choose to use it, is a tool to provide you with how often devices you select have failed to report an updated temperature in a time frequency you selected. I highly recommend you do NOT tie this into audio alarms going off, red lights flashing, or even entering the self-destruct code! I let the other Hubitat Apps that monitor temperature “values” do that in my setup.'
	myDevStrings += '<p style="font-size:18px; letter-spacing: 1px; line-height: 1.4;">There are several great apps that will check on device activity, check on device temperature values, etc…, that do a great job. But, over the years I have seen too many times when a device reports activity but there has not been a temperature reported over an abnormal span of time. This app is solely focused on checking the last time a temperature was reported, plain and simple!'
	myDevStrings += '<p style="font-size:18px; letter-spacing: 1px; line-height: 1.4;"><u>Disclaimer:</u> I will not recommend devices because what may work for me in my environment, most likely will not work the same for other user’s environments. '     
    myDevStrings += '<h2 style="color: red;">App Instructions</h2>'
    myDevStrings += '<p style="font-size:18px; letter-spacing: 1px; line-height: 1.4;">Show app instructions – what you are seeing now<br>'
	myDevStrings += '<p style="font-size:18px; letter-spacing: 1px; line-height: 1.4;">Create a new App with a Group of Devices'
    myDevStrings += '<ul>'
    myDevStrings += '<li style="font-size:18px; letter-spacing: 1px; line-height: 1.4;">Create a Name for the new Group'
    myDevStrings += '<li style="font-size:18px; letter-spacing: 1px; line-height: 1.4;">Select the Devices to Monitor'
    myDevStrings += '<li style="font-size:18px; letter-spacing: 1px; line-height: 1.4;">Select the Frequency to Check that a temperature has been reported'
    myDevStrings += '<li style="font-size:18px; letter-spacing: 1px; line-height: 1.4;">Decided if you would like a Notification'
    myDevStrings += '<ul>'
    myDevStrings += '<li style="font-size:18px; letter-spacing: 1px; line-height: 1.4;">If so, decide Not to get a notifications during a set period of time'
    myDevStrings += '</ul>'
    myDevStrings += '<li style="font-size:18px; letter-spacing: 1px; line-height: 1.4;">Decide if you would like a log entry when a check fails'
    myDevStrings += '<ul>'
    myDevStrings += '<li style="font-size:18px; letter-spacing: 1px; line-height: 1.4;">If so, decide if you want the log entry to be an Error or a Warning'
    myDevStrings += '<li style="font-size:18px; letter-spacing: 1px; line-height: 1.4;">when selected (on) an log.Error is created, if (off) a log.Warn is created'
    myDevStrings += '</ul>'
    myDevStrings += '<li style="font-size:18px; letter-spacing: 1px; line-height: 1.4;">Decide if you would like App logging where log.Info items are created to show what is reported, etc…'
    myDevStrings += '<li style="font-size:18px; letter-spacing: 1px; line-height: 1.4;">Select Debug logging if you are into pain, or the app has logged/reported a programing or a device error you have selected to use'
    myDevStrings += '<li style="font-size:18px; letter-spacing: 1px; line-height: 1.4;">Once you have created a group (completed the required fields and hit “Done”).'
    myDevStrings += '<ul>'
    myDevStrings += '<li style="font-size:18px; letter-spacing: 1px; line-height: 1.4;">When you go back in you will see a list of Device Temperature Events'
    myDevStrings += '<li style="font-size:18px; letter-spacing: 1px; line-height: 1.4;">When the group is first created the events will be the time you created the group, from then on, they will be the actual last reported temperature'
    myDevStrings += '</ul>'
    myDevStrings += '</ul>'
    myDevStrings += '<br>'
    myDevStrings += '<p style="font-size:18px; letter-spacing: 1px; line-height: 1.4;">I highly recommend you go slow. When you create the first groups, do not go crazy and turn all of the options on until you have had a chance to see how all of this works. I have 55+ sensors that report temperature and only monitor 16. Once I started monitoring for temperature updates and time frames, I had to swap a few sensors to different locations to get what I wanted. Good Luck.'     
	myDevStrings += '</table>'
    myDevStrings += '</td></tr>'
    myDevStrings += '</table>'  //must be last line

    return myDevStrings
}

def logMsg(msg) 
{
	log.info msg
}
