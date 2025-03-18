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
*/

definition(
    name: "Device Temperature Check",
    namespace: "myHubitat",
    author: "Scott Wade",
    description: "Notify if Device Temperature not reported in X minutes",
    category: "Utility",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "")

preferences {
	page(name: "mainAppPage" )
}

def mainAppPage() {
    dynamicPage(name: "mainAppPage", install: true, uninstall: true) {
       	section("") {
            
            app name: "DTCchildApp", appName: "DTCchildApp", namespace: "myHubitat", title: "Add New Group of Devices", multiple: true
		}
    }
}    

def installed() {
	logMsg("Installed App")
	initialize()
}

def updated() {
    initialize()
}

def uninstalled() {
	logMsg("Uninstalled App")
    // unsubscribe to all events
    unschedule()
    unsubscribe()
}

def initialize() {
}

def logMsg(msg) 
{
	log.info "DTC: " + msg
}
