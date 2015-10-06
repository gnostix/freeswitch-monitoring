package gr.gnostix.freeswitch.actors

import java.sql.Timestamp

/**
 * Created by rebel on 23/8/15.
 */

sealed trait EventType {
  def eventName: String
}

case class HeartBeat(eventName: String, eventInfo: String, uptimeMsec: Long, sessionCount: Int,
                     sessionPerSecond: Int, eventDateTimestamp: Timestamp, cpuUsage: Double, sessionPeakMax: Int,
                     sessionPeakMaxFiveMin: Int, freeSWITCHHostname: String, freeSWITCHIPv4: String, upTime: String)
  extends EventType


sealed trait CallEventType extends EventType {
  def fromUser: String

  def toUser: String
}

case class CallNew(uuid: String, eventName: String, fromUser: String, toUser: String, readCodec: String, writeCodec: String,
                   fromUserIP: String, toUserIP: String, callUUID: String, callerChannelCreatedTime: Option[Timestamp],
                   callerChannelAnsweredTime: Option[Timestamp], freeSWITCHHostname: String, freeSWITCHIPv4: String,
                    callDirection: String, pdd: Float, ringingSec: Float)
  extends CallEventType


case class CallEnd(uuid: String, eventName: String, fromUser: String, toUser: String, readCodec: String, writeCodec: String,
                   fromUserIP: String, toUserIP: String, callUUID: String, callerChannelCreatedTime: Option[Timestamp],
                   callerChannelAnsweredTime: Option[Timestamp], callerChannelHangupTime: Timestamp,
                   freeSWITCHHostname: String, freeSWITCHIPv4: String, hangupCause: String,  billSec: Int,
                   rtpQualityPerc: Double, otherLegUniqueId: String, hangupDisposition: String, callDirection: String,
                    mos: Double, pdd: Float, ringingSec: Float)
  extends CallEventType

case class FailedCall(eventName: String, fromUser: String, toUser: String, callUUID: String, freeSWITCHIPv4: String)
  extends CallEventType

case class OtherEvent(eventName: String, uuid: String) extends EventType


