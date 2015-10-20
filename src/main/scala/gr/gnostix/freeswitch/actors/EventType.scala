/*
 * Copyright (c) 2015 Alexandros Pappas p_alx hotmail com
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package gr.gnostix.freeswitch.actors

import java.sql.Timestamp

/**
 * Created by rebel on 23/8/15.
 */

sealed trait EventType {
  def eventName: String
}

case class HeartBeat(eventName: String, eventInfo: String, uptimeMsec: Long, concurrentCalls: Int,
                     sessionPerSecond: Int, eventDateTimestamp: Timestamp, cpuUsage: Double, callsPeakMax: Int,
                     sessionPeakMaxFiveMin: Int, freeSWITCHHostname: String, freeSWITCHIPv4: String, upTime: String,
                     maxAllowedCalls: Int)
  extends EventType

case class AvgHeartBeat(eventName: String, uptimeMsec: Long, concurrentCalls: Int, sessionPerSecond: Int,
                        eventDateTimestamp: Timestamp, cpuUsage: Double, callsPeakMax: Int, sessionPeakMaxFiveMin: Int,
                        maxAllowedCalls: Int)
  extends EventType


sealed trait CallEventType extends EventType {
  def fromUser: String

  def toUser: String
}

case class CallNew(uuid: String, eventName: String, fromUser: String, toUser: String, readCodec: String, writeCodec: String,
                   fromUserIP: String, toUserIP: String, callUUID: String, callerChannelCreatedTime: Option[Timestamp],
                   callerChannelAnsweredTime: Option[Timestamp], freeSWITCHHostname: String, freeSWITCHIPv4: String,
                   callDirection: String, pdd: Float, ringingSec: Float, dialCode: Option[String], country: Option[String])
  extends CallEventType


case class CallEnd(uuid: String, eventName: String, fromUser: String, toUser: String, readCodec: String, writeCodec: String,
                   fromUserIP: String, toUserIP: String, callUUID: String, callerChannelCreatedTime: Option[Timestamp],
                   callerChannelAnsweredTime: Option[Timestamp], callerChannelHangupTime: Timestamp,
                   freeSWITCHHostname: String, freeSWITCHIPv4: String, hangupCause: String, billSec: Int,
                   rtpQualityPerc: Double, otherLegUniqueId: String, hangupDisposition: String, callDirection: String,
                   mos: Double, pdd: Float, ringingSec: Float, dialCode: Option[String], country: Option[String])
  extends CallEventType

case class FailedCall(eventName: String, fromUser: String, toUser: String, callUUID: String, freeSWITCHIPv4: String)
  extends CallEventType

case class OtherEvent(eventName: String, uuid: String) extends EventType


