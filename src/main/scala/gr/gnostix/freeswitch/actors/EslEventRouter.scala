package gr.gnostix.freeswitch.actors

import java.sql.Timestamp

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{OneForOneStrategy, Props, Actor, ActorLogging}
import gr.gnostix.freeswitch.actors.ActorsProtocol.{GetAllHeartBeat, GetLastHeartBeat, Event}


/**
 * Created by rebel on 23/8/15.
 */
class EslEventRouter extends Actor with ActorLogging {


  override val supervisorStrategy =
    OneForOneStrategy() { //?????????????? do I need this ?
      case _ => Restart
    }

  val callRouterActor = context.actorSelection("/user/centralMessageRouter/callRouter")
  val heartBeatActor = context.actorSelection("/user/centralMessageRouter/heartBeatActor")


  def receive: Receive = {
    case Event(headers) =>

      getCallEventType(headers) match {
        case x @ CallNew(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, toUserIP, callUUID,
        callerChannelCreatedTime, callerChannelAnsweredTime, freeSWITCHHostname, freeSWITCHIPv4, callDirection, pdd, ringTimeSec)
          if callUUID != "_UNKNOWN" =>
          callRouterActor ! x

        case x@CallNew(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, toUserIP, callUUID,
        callerChannelCreatedTime, callerChannelAnsweredTime, freeSWITCHHostname, freeSWITCHIPv4, callDirection, pdd, ringTimeSec) =>
          log info "_UNKNOWN" + x.toString

        case x @ CallEnd(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, toUserIP, callUUID,
        callerChannelCreatedTime, callerChannelAnsweredTime, callerChannelHangupTime, freeSWITCHHostname,
        freeSWITCHIPv4, hangupCause, billSec, rtpQualityPerc, otherLegUniqueId, hangupDisposition, callDirection, mos,
        pdd, ringTimeSec)
          if x.callUUID != "_UNKNOWN" =>
          log info "-----> " + x.toString
          callRouterActor ! x

        case x@CallEnd(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, toUserIP, callUUID,
        callerChannelCreatedTime, callerChannelAnsweredTime, callerChannelHangupTime, freeSWITCHHostname,
        freeSWITCHIPv4, hangupCause, billSec, rtpQualityPerc, otherLegUniqueId, hangupDisposition, callDirection, mos,
        pdd, ringTimeSec) =>
          log info s"no uuid $uuid" + x.toString

        case x @ HeartBeat(eventType, eventInfo, uptimeMsec, sessionCount, sessionPerSecond, eventDateTimestamp,
        idleCPU, sessionPeakMax, sessionPeakMaxFiveMin, freeSWITCHHostname, freeSWITCHIPv4, upTime) =>

          heartBeatActor ! x

        //log info "BEAT " + x.toString()
        case x@OtherEvent(name, uuid) =>
        // log info x.toString
      }


    case x@ (GetLastHeartBeat | GetAllHeartBeat) =>
      heartBeatActor forward x


  }

  def getCallEventType(headers: scala.collection.Map[String, String]): EventType = {

    (headers get "Event-Name") match {
      case Some("CHANNEL_ANSWER") =>
        val newCall = getCallEventTypeChannelCall(headers)
        newCall

      case Some("CHANNEL_HANGUP_COMPLETE") =>
        val endCall = getCallEventTypeChannelCall(headers)
        endCall

      case Some("HEARTBEAT") =>
        val heartBeat = getEventHeartbeat(headers)
        heartBeat

      case Some(x) =>
        val uuid = headers get "Unique-ID" getOrElse "_UNKNOWN"
        OtherEvent(x, uuid)

      case None =>
        val uuid = headers get "Unique-ID" getOrElse "_UNKNOWN"
        OtherEvent("_UNKNOWN", uuid)
    }
  }
  
  
  
  def getCallEventTypeChannelCall(headers: scala.collection.Map[String, String]): EventType = {
    //val formatter: DateTimeFormatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss");
    val formatter = "dd/MM/yyyy HH:mm:ss"
    // Channel-Call-UUID is unique for call but since we have one event for every channel it comes twice
    // Unique-ID is unique per channel. In every call we have two channels
    val uuid = headers get "Unique-ID" getOrElse "_UNKNOWN"
    val eventName = headers get "Event-Name" getOrElse "_UNKNOWN"
    val fromUser = headers get "Caller-Caller-ID-Number" getOrElse "_UNKNOWN"
    val toUser = headers get "Caller-Destination-Number" getOrElse "_UNKNOWN"
    val readCodec = headers get "Channel-Read-Codec-Name" getOrElse "_UNKNOWN"
    val writeCodec = headers get "Channel-Write-Codec-Name" getOrElse "_UNKNOWN"
    val fromUserIP = headers get "Caller-Network-Addr" getOrElse "_UNKNOWN"
    val toUserIP = headers get "Other-Leg-Network-Addr" getOrElse "_UNKNOWN"
    val freeSWITCHHostname = headers get "FreeSWITCH-Hostname" getOrElse "0"
    val freeSWITCHIPv4 = headers get "FreeSWITCH-IPv4" getOrElse "0"
    val callDirection = headers get "Call-Direction" getOrElse "_UNKNOWN"
    val callUUID = headers get "Channel-Call-UUID" getOrElse "_UNKNOWN"

    val callerChannelCreatedTime = headers get "Caller-Channel-Created-Time" getOrElse "0" match {
      case "0" => None
      case x => Some(new Timestamp(x.toLong / 1000))
    }

    val callerChannelAnsweredTime = headers get "Caller-Channel-Answered-Time" getOrElse "0" match {
      case "0" => None
      case x => Some(new Timestamp(x.toLong / 1000))
    }

    val varStartStamp = headers get "variable_start_uepoch" getOrElse "0" match {
      case "0" => 0
      case x => x.toLong
    }

    val varProgressEpoch = headers get "variable_progress_uepoch" getOrElse "0" match {
      case "0" => 0
      case x => x.toLong
    }

    val varAnswerEpoch = headers get "variable_answer_uepoch" getOrElse "0" match {
      case "0" => 0
      case x => x.toLong
    }

    val pdd = getPDD(varProgressEpoch, varStartStamp)
    val ringTimeSec = getRingTimeSec(varAnswerEpoch, varProgressEpoch)


    eventName match {
      case "CHANNEL_ANSWER" =>

        CallNew(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, toUserIP, callUUID,
          callerChannelCreatedTime, callerChannelAnsweredTime, freeSWITCHHostname, freeSWITCHIPv4,
          callDirection, pdd, ringTimeSec)

      case "CHANNEL_HANGUP_COMPLETE" =>
        val otherLegUniqueId = headers get "Other-Leg-Unique-ID" getOrElse "_UNKNOWN"
        val hangupCause = headers get "Hangup-Cause" getOrElse "_UNKNOWN"
        val billSec = headers get "variable_billsec" getOrElse "0"
        val rtpQualityPerc = headers get "variable_rtp_audio_in_quality_percentage" getOrElse "0"
        val hangupDisposition = headers get "variable_sip_hangup_disposition" getOrElse "_UNKNOWN"
        val mos = headers get "variable_rtp_audio_in_mos" getOrElse "0"

        val callerChannelHangupTime =  headers get "Caller-Channel-Hangup-Time" getOrElse "0" match {
          case "0" => new Timestamp(System.currentTimeMillis) // if the call failed this value would be 0 but we need the time that the call failed
          case x => new Timestamp(x.toLong / 1000)
        }

        CallEnd(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, toUserIP, callUUID, callerChannelCreatedTime,
          callerChannelAnsweredTime, callerChannelHangupTime, freeSWITCHHostname, freeSWITCHIPv4, hangupCause,
          billSec.toInt, rtpQualityPerc.toDouble, otherLegUniqueId, hangupDisposition, callDirection, mos.toDouble,
          pdd, ringTimeSec)

    }

  }

  def getEventHeartbeat(headers: scala.collection.Map[String, String]): EventType = {
    //val uuid = "_UNKNOWN"
    val eventInfo = headers get "Event-Info" getOrElse "_UNKNOWN"
    val upTime = headers get "Up-Time" getOrElse "_UNKNOWN"
    val sessionCount = headers get "Session-Count" getOrElse "0"
    val sessionPerSecond = headers get "Session-Per-Sec" getOrElse "0"
    val cpuUsage = 100 - (headers get "Idle-CPU" getOrElse "0").toDouble
    val sessionPeakMax = headers get "Session-Peak-Max" getOrElse "0"
    val sessionPeakMaxFiveMin = headers get "Session-Peak-FiveMin" getOrElse "0"
    val freeSWITCHHostname = headers get "FreeSWITCH-Hostname" getOrElse "0"
    val freeSWITCHIPv4 = headers get "FreeSWITCH-IPv4" getOrElse "0"
    val uptimeMsec = headers get "Uptime-msec" getOrElse "0"

    val eventDateTimestamp =  headers get "Event-Date-timestamp" getOrElse "0" match {
      case "0" => new Timestamp(System.currentTimeMillis) // if the call failed this value would be 0 but we need the time that the call failed
      case x => new Timestamp(x.toLong / 1000)
    }

    HeartBeat("HEARTBEAT", eventInfo, uptimeMsec.toLong, sessionCount.toInt, sessionPerSecond.toInt,
      eventDateTimestamp, BigDecimal(cpuUsage).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble,
      sessionPeakMax.toInt, sessionPeakMaxFiveMin.toInt, freeSWITCHHostname, freeSWITCHIPv4, upTime)
  }

def getPDD(varProgressEpoch: Long, varStartStamp: Long): Float = {
  // PDD calculation
  (varProgressEpoch - varStartStamp) / 1000 / 1000 // 1000 to milliseconds and / 1000 to seconds
}

  def getRingTimeSec(varAnswerEpoch: Long, varProgressEpoch: Long): Float = {
    // PDD calculation
    (varAnswerEpoch - varProgressEpoch) / 1000 / 1000 // 1000 to milliseconds and / 1000 to seconds
  }

}


