/**
 *  Lock it at a specific time
 *
 *  Copyright 2014 Erik Thayer
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
    name: "Make sure it is locked",
    namespace: "user8798",
    author: "Erik Thayer",
    description: "Make sure a door is locked at a specific time.  Option to add door contact sensor to only lock if closed.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)


preferences {
  section("At this time every day") {
    input "time", "time", title: "Time of Day"
  }
  section("On these days") {
    input "days", "enum", multiple: true, title: "Days of week", metadata:[values:["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]], required: false
  }
  section("Make sure this is locked") {
    input "lock","capability.lock"
  }
  section("Make sure it's closed first..."){
    input "contact", "capability.contactSensor", title: "Which contact sensor?", required: false
  }
  section( "Notifications" ) {
    input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes", "No"]], required: false
    input "phone", "phone", title: "Send a text message?", required: false
  } 
}
import java.text.SimpleDateFormat
def installed() {
  def tz = location.timeZone
  def timePart = timeToday(time, tz).format("s m H", tz);
  def daysOfWeek
  if (days.length() > 0) {
    daysOfWeek = ""
    for (dayOfWeek in days) {
      daysOfWeek += dayToNum(dayOfWeek) + ","
    }
  } else {
    daysOfWeek = "*"
  }
  daysOfWeek = daysOfWeek.substring(0, daysOfWeek.length() - 1)
  log.info daysOfWeek
  def crontab = timePart + " ? * " + daysOfWeek
  log.info crontab
  schedule(crontab, "setTimeCallback")
  log.info time
}

def dayToNum(day) {
  def daysToNum = [:]
  daysToNum.'Sunday' = 1
  daysToNum.'Monday' = 2
  daysToNum.'Tuesday' = 3
  daysToNum.'Wednesday' = 4
  daysToNum.'Thursday' = 5
  daysToNum.'Friday' = 6
  daysToNum.'Saturday' = 7
  
  daysToNum[day]
}

def updated(settings) {
  unschedule()
  installed()
}

def setTimeCallback() {
  if (contact) {
    doorOpenCheck()
  } else {
    lockMessage()
    lock.lock()
  }
}
def doorOpenCheck() {
  def currentState = contact.contactState
  if (currentState?.value == "open") {
    def msg = "${contact.displayName} is open.  Scheduled lock failed."
    log.info msg
    sendPush msg
    if (phone) {
      sendSms phone, msg
    }
  } else {
    lockMessage()
    lock.lock()
  }
}

def lockMessage() {
  def msg = ""
  if (lock.lock == "locked") {
  	msg = "Locking ${lock.displayName} due to scheduled lock."
  } else {
    msg = "${lock.displayName} is already locked"
  }
  log.info msg
  if (sendPushMessage) {
    sendPush msg
  }
  if (phone) {
    sendSms phone, msg
  }
}