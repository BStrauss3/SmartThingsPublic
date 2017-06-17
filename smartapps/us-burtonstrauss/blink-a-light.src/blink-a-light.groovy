/**
 *  Blink A Light
 *
 *  Copyright 2017 Burton Strauss III
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

definition(
    name: "Blink A Light",
    namespace: "us.burtonstrauss",
    author: "Burton Strauss III",
    description: "Blink a light",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png") {
}


preferences {
	section("Light") {
		paragraph "Which light(s) should I blink?"
        input "lights", "capability.Light", title: "Lights?", multiple: true        
	}
	section(hideable: true, hidden: true, "Blink") {
        paragraph "(Optional) values for how many times to blink and how long."
        input "blinkCountLimit", "number", required: true, title: "Count (2..10)?", defaultValue:  2, range: "2..10"
        input "blinkInterval", "number", required: true, title: "Interval (seconds) (30..120)?", defaultValue:  30, range: "30..120"
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}" 
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	log.debug "Initializing with settings: ${settings}"
    
    if ( blinkCountLimit > 10 ) {
    	state.blinkCountLimit = 10
	} else if ( blinkCountLimit < 1) {
    	state.blinkCountLimit = 1
    } else {
    	state.blinkCountLimit = BlinkCount
	}

    if ( blinkInterval > 120 ) {
    	state.blinkInterval = 120
	} else if ( blinkInterval < 30) {
    	state.blinkInterval = 30
	} else {
    	state.blinkInterval = blinkInterval
    }
    // initialize counter
    state.blinkCounter = 0
    
    // schedule the turn on and turn off handlers
    runIn(1, turnOnHandler)

}

// simple turn on light handler
def turnOnHandler() {
	log.debug "Turn ON: ${state.blinkCounter}"
    lights.on()
    runIn(state.blinkInterval, turnOffHandler)    
	log.debug "Turn OFF scheduled"
}

// simple turn off lights handler
def turnOffHandler() {
	log.debug "Turn OFF: ${state.blinkCounter}"
    lights.off()
    state.blinkCounter = state.blinkCounter + 1
    if (state.blinkCounter < state.blinkCountLimit) {
        runIn(state.blinkInterval, turnOnHandler)    
        log.debug "Turn ON scheduled"
    } else {
        log.debug "Blink ${lights} completed"
    }

}
