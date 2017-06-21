/**
 *  Alert Blink
 *
 *  Copyright 2017 Strauss (42R)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 
 * Merged code from "The Flasher II" to make the flashing work...
 *    https://gist.github.com/jmdaly/02367112114ce1dbe972
 
 *
 */
definition(
    name: "Alert Blink",
    namespace: "us.burtonstrauss",
    author: "Burton Strauss III",
    description: "This app is intended to be run between specific times (e.g. 10:30pm until 5:00am) and if a contact is open (e.g. the Garage Door), then blink a light as an alert",
    category: "Safety & Security",
    iconUrl: "http://cdn.device-icons.smartthings.com/lights/multi-light-bulb-on.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/lights/multi-light-bulb-on@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/lights/multi-light-bulb-on@3x.png"   
)

preferences {
  page(name: "page 1", title: "Sensors/Lights", nextPage: "page 2", install: false, uninstall: true) {
    section("Sensor") {
		paragraph "Which sensor(s)?"
        input "sensors", "capability.contactSensor", title: "Sensors?", multiple: true, required: true 
// TODO: If you only have one sensor, pre-select it        
    } 
    section("Light") {
		paragraph "Which light(s) should I blink?"
        input "lights", "capability.Light", title: "Lights?", multiple: true, required: true
	} 
  }
  page(name: "page 2", title: "Hours", nextPage: "page 3", install: false, uninstall: true) {
	section("Time") {
		paragraph "Between which hours?"
//TODO this doesn't work, when installing it sets both to current time regardless...
        input "fromTime", "time", title: "From (10:30pm) ?", required: false, description: "9:00 AM", default: "9:00 AM", defaultValue: "9:00 AM"
        input "toTime", "time", title: "To (5am)?", required: false, description: "11:00 AM", default: "11:00 AM", defaultValue: "11:00 AM"
	} 
  }
  page(name: "page 3", title: "Options", install: true, uninstall: true) {
	section("Blink") {
        paragraph "(Optional) values for how many times to blink and how long."
        input "blinkCount", "number", title: "Count (5..30)?", required: false, defaultValue: "5", range: "(5..30)"
        input "blinkInterval", "number", title: "BlinkInterval (seconds) (2..15)?", required: false, defaultValue:  "5", range: "(2..15)"
    }
    section("Check") {
        input "checkInterval", "number", title: "CheckInterval (seconds) (600..3600)?", required: false, defaultValue: "900", range: "(600..3600)"
    }
    section("Label") {
        label(name: "label", title: "optional app label", required: false, multiple: false)
    }
  }
}

def installed() {
    log.debug "Installed...with settings: ${settings}"   
	initialize()
}

def updated() {
	log.debug "Updated...with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	log.debug "Initializing...with settings: ${settings}"
  
    if (fromTime == Null) {
//ERROR these do not work!    
        state.fromTime = (new Date().set(hourOfDay: 22, minute: 30))
        log.debug "...fromTime: ${state.fromTime} default"
    } else {
    	state.fromTime = fromTime
        log.debug "...fromTime: ${state.fromTime} from ${fromTime}"
    }
    
    if (toTime == Null) {
//ERROR these do not work!    
        state.toTime = (new Date().set(hourOfDay: 5, minute: 0))
        log.debug "...toTime: ${state.toTime} default"
    } else {
    	state.toTime = toTime
        log.debug "...toTime: ${state.toTime} from ${toTime}"
    }

    if (blinkCount == Null) {
		state.blinkCountLimit = 5
		log.debug "...blinkCountLimit: 5 (default)"
    } else {
		// why this fails? state.blinkCountLimit = Math.Max(30,Math.Min(5,blinkCount))
        if (blinkCount < 5) {
            state.blinkCountLimit = 5
        } else if (blinkCount > 30) {
            state.blinkCountLimit = 30
        } else {
            state.blinkCountLimit = blinkCount
        }
		log.debug "...blinkCountLimit: ${state.blinkCountLimit} from ${blinkCount}"
    }

    if (blinkInterval == Null) {
		state.blinkInterval = 5
		log.debug "...blinkInterval: 5 (default)"
    } else {
        // state.blinkInterval = Math.Max(15,Math.Min(2,blinkInterval))
        if (blinkInterval < 2) {
            state.blinkInterval = 2
        } else if (blinkInterval > 15) {
            state.blinkInterval = 15
        } else {
            state.blinkInterval = blinkInterval
        }
		log.debug "...blinkInterval: ${state.blinkInterval} from ${blinkInterval}"
    }

    if (checkInterval == Null) {
		state.checkInterval = 900
		log.debug "...checkInterval: 900 (default)"
    } else {
        // state.checkInterval = Math.Max(600,Math.Min(3600,checkInterval))
        if (checkInterval < 600) {
            state.checkInterval = 600
        } else if (checkInterval > 3600) {
            state.checkInterval = 3600
        } else {
            state.checkInterval = checkInterval
        }
		log.debug "...checkInterval: ${state.checkInterval} from ${checkInterval}"
    }

    // initialize counter
    state.blinkCounter = 0
	log.debug "Initialized"

	subscribe(sensors, "contact.open", sensorOpenHandler)
}

def sensorOpenHandler(evt) {

	log.debug "Starting sensorOpenHandler(${evt}) at ${new Date()} for ${state.fromTime} ${state.toTime} ${location.timeZone}"
    
    // Are we between the times?
	def between = timeOfDayIsBetween(fromTime, toTime, new Date(), location.timeZone)
	if (between) {
    	log.debug "...Inside window... flashing"
		flashLights()
    } else {
    	log.debug "...Outside of window... nop"
    }
}

private flashLights() { 
 	def doFlash = true 
 
 	log.debug "LAST ACTIVATED IS: ${state.lastActivated}" 
 	if (state.lastActivated) { 
 		def elapsed = now() - state.lastActivated 
        log.debug "DO FLASH: ELAPSED: $elapsed since LAST ACTIVATED: ${state.lastActivated}" 
 		doFlash = elapsed > (1000L * state.checkInterval)
        if (doFlash) {
	 		def sequenceTime = 1000L * (state.blinkCountLimit + 1) * (state.blinkInterval + state.blinkInterval)
 			doFlash = elapsed > sequenceTime
            if (!doFlash) {
	            log.debug "Skipping flash because of sequence interval"
            }
        } else {
            log.debug "Skipping flash because of CheckInterval"
        }
 	} 
 
 	if (doFlash) { 
        log.debug "FLASHING $state.blinkCountLimit times" 
 		state.lastActivated = now() 
 		log.debug "LAST ACTIVATED SET TO: ${state.lastActivated}" 

 		def delay = 0L

	 	lights.eachWithIndex {l, i -> 
        	log.trace "Switch $l off initially"
			l.on(delay: delay) 
        }

		state.blinkCountLimit.times { 
 		    delay += state.blinkInterval * 1000L 
 			log.trace "Switch $l on after  $delay ms" 
 			lights.eachWithIndex {l, i -> 
  					l.on(delay: delay) 
            }

			delay += state.blinkInterval * 1000L 
            log.trace "Switch $l off after $delay ms" 
 			lights.eachWithIndex {l, i -> 
 					l.off(delay: delay) 
            }
 		} 
 	} 
}
