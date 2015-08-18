package gr.gnostix.freeswitch.actors

import java.sql.Timestamp

import akka.actor.SupervisorStrategy.Restart
import akka.actor._
import org.freeswitch.esl.client.transport.event.EslEvent
import org.joda.time.{LocalDateTime, DateTime}
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.json4s.JsonAST.{JObject, JString}
import org.scalatra.atmosphere.{AtmosphereClient, JsonMessage, TextMessage}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global

import ActorsJsonProtocol._

sealed trait EventType {
  def eventName: String
}

case class HeartBeat(eventName: String, eventInfo: String, upTime: String, uptimeMsec: Long, sessionCount: Int,
                     sessionPerSecond: Int, eventDateTimestamp: Timestamp, idleCPU: String, sessionPeakMax: Int,
                     sessionPeakMaxFiveMin: Int, freeSWITCHHostname: String, freeSWITCHIPv4: String)
 extends EventType

case class ChatHeartBeat(author: String, message: String)

sealed trait CallEventType extends EventType {
  def fromUser: String

  def toUser: String
}

case class CallNew(uuid: String, eventName: String, fromUser: String, toUser: String, readCodec: String, writeCodec: String,
                   fromUserIP: String, callUUID: String, callerChannelCreatedTime: Option[Timestamp],
                   callerChannelAnsweredTime: Option[Timestamp], freeSWITCHHostname: String, freeSWITCHIPv4: String)
  extends CallEventType


case class CallEnd(uuid: String, eventName: String, fromUser: String, toUser: String, readCodec: String, writeCodec: String,
                   fromUserIP: String, callUUID: String, callerChannelCreatedTime: Option[Timestamp],
                   callerChannelAnsweredTime: Option[Timestamp], callerChannelHangupTime: Timestamp,
                   freeSWITCHHostname: String, freeSWITCHIPv4: String, hangupCause: String,  billSec: Int, rtpQualityPerc: Double)
  extends CallEventType

case class FailedCall(eventName: String, fromUser: String, toUser: String, callUUID: String, freeSWITCHIPv4: String)
  extends CallEventType

case class CallOther(eventName: String, uuid: String) extends EventType



object CallRouter {

  sealed trait RouterProtocol

  sealed trait RouterRequest extends RouterProtocol

  sealed trait RouterResponse extends RouterProtocol

  case class Event(headers: scala.collection.Map[String, String]) extends RouterRequest

  case object GetCalls extends RouterRequest

  case object GetTotalFailedCalls extends RouterRequest

  case object GetFailedCalls extends RouterRequest

  case class GetFailedCallsByDate(from: Timestamp, to: Timestamp) extends RouterRequest

  case class GetCallsResponse(totalCalls: Int, activeCallsUUID: List[String]) extends RouterResponse

  case class GetCallInfo(uuid: String) extends RouterRequest

  case class GetChannelInfo(callUuid: String, channelUuid: String) extends RouterRequest


  object Event {
    def apply(event: EslEvent): Event = Event(event.getEventHeaders.asScala)

    def apply(): Event = Event(scala.collection.Map.empty[String, String])
  }

  def mkEvent(event: EslEvent): Event = Event(event)
}


class CallRouter extends Actor with ActorLogging {

  import gr.gnostix.freeswitch.actors.CallRouter._

  override val supervisorStrategy =
    OneForOneStrategy() {
      case _ => Restart
    }

  // create the FailedCallsActor in advance
  val failedCallsActor = context.actorOf(Props[FailedCallsActor], "failedCallsActor")



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
    val callUUID = headers get "Channel-Call-UUID" getOrElse "_UNKNOWN"
    val freeSWITCHHostname = headers get "FreeSWITCH-Hostname" getOrElse "0"
    val freeSWITCHIPv4 = headers get "FreeSWITCH-IPv4" getOrElse "0"

    val callerChannelCreatedTime = headers get "Caller-Channel-Created-Time" getOrElse "0" match {
      case "0" => None
      case x => Some(new Timestamp(x.toLong / 1000))
    }

    val callerChannelAnsweredTime = headers get "Caller-Channel-Answered-Time" getOrElse "0" match {
      case "0" => None
      case x => Some(new Timestamp(x.toLong / 1000))
    }

    eventName match {
      case "CHANNEL_ANSWER" => CallNew(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, callUUID,
        callerChannelCreatedTime, callerChannelAnsweredTime, freeSWITCHHostname, freeSWITCHIPv4)
      case "CHANNEL_HANGUP_COMPLETE" =>
        val hangupCause = headers get "Hangup-Cause" getOrElse "_UNKNOWN"
        val billSec = headers get "variable_billsec" getOrElse "0"
        val rtpQualityPerc = headers get "variable_rtp_audio_in_quality_percentage" getOrElse "0"

        val callerChannelHangupTime =  headers get "Caller-Channel-Hangup-Time" getOrElse "0" match {
          case "0" => new Timestamp(System.currentTimeMillis) // if the call failed this value would be 0 but we need the time that the call failed
          case x => new Timestamp(x.toLong / 1000)
        }

        CallEnd(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, callUUID, callerChannelCreatedTime,
          callerChannelAnsweredTime, callerChannelHangupTime, freeSWITCHHostname, freeSWITCHIPv4, hangupCause,
        billSec.toInt, rtpQualityPerc.toDouble)

    }

  }

  def getEventHeartbeat(headers: scala.collection.Map[String, String]): EventType = {
    //val uuid = "_UNKNOWN"
    val eventInfo = headers get "Event-Info" getOrElse "_UNKNOWN"
    val upTime = headers get "Up-Time" getOrElse "_UNKNOWN"
    val sessionCount = headers get "Session-Count" getOrElse "0"
    val sessionPerSecond = headers get "Session-Per-Sec" getOrElse "0"
    val idleCPU = headers get "Idle-CPU" getOrElse "0"
    val sessionPeakMax = headers get "Session-Peak-Max" getOrElse "0"
    val sessionPeakMaxFiveMin = headers get "Session-Peak-FiveMin" getOrElse "0"
    val freeSWITCHHostname = headers get "FreeSWITCH-Hostname" getOrElse "0"
    val freeSWITCHIPv4 = headers get "FreeSWITCH-IPv4" getOrElse "0"
    val uptimeMsec = headers get "Uptime-msec" getOrElse "0"

    val eventDateTimestamp =  headers get "Event-Date-timestamp" getOrElse "0" match {
      case "0" => new Timestamp(System.currentTimeMillis) // if the call failed this value would be 0 but we need the time that the call failed
      case x => new Timestamp(x.toLong / 1000)
    }

    HeartBeat("HEARTBEAT", eventInfo, upTime, uptimeMsec.toLong, sessionCount.toInt, sessionPerSecond.toInt,
      eventDateTimestamp, idleCPU, sessionPeakMax.toInt,
      sessionPeakMaxFiveMin.toInt, freeSWITCHHostname, freeSWITCHIPv4)
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
        CallOther(x, uuid)

      case None =>
        val uuid = headers get "Unique-ID" getOrElse "_UNKNOWN"
        CallOther("_UNKNOWN", uuid)
    }
  }


  def idle(activeCalls: scala.collection.Map[String, ActorRef]): Receive = {
    case Event(headers) =>

      getCallEventType(headers) match {
        case x @ CallNew(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, callUUID,
        callerChannelCreatedTime, callerChannelAnsweredTime, freeSWITCHHostname, freeSWITCHIPv4)
          if callUUID != "_UNKNOWN" =>
          log info x.toString

           (activeCalls get callUUID) match {
            case None =>

              // we send once the call info
              //AtmosphereClient.broadcast("/live/events", x)

              //val actor = context actorOf CallActor
              val actor = context.actorOf(Props[CallActor], callUUID)
              actor ! x
              context watch actor

              val newMap = activeCalls updated(callUUID, actor)
              context become idle(newMap)
            case Some(actor) =>
              actor ! x
              log info s"Call $uuid already active, sending the second channel .."
          }

        case x@CallNew(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, callUUID,
        callerChannelCreatedTime, callerChannelAnsweredTime, freeSWITCHHostname, freeSWITCHIPv4) =>
          log info "_UNKNOWN" + x.toString

        case x @ CallEnd(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, callUUID,
        callerChannelCreatedTime, callerChannelAnsweredTime, callerChannelHangupTime, freeSWITCHHostname,
        freeSWITCHIPv4, hangupCause, billSec, rtpQualityPerc) if callUUID != "_UNKNOWN" =>
          log info "-----> " + x.toString

          (activeCalls get callUUID) match {
            case None =>

              x.callerChannelAnsweredTime match {
                case None => failedCallsActor ! x
                case Some(a) => log info s"Call $uuid doesn't exist! with answered time " + x.callerChannelAnsweredTime
              }
              log info s"Call $uuid doesn't exist!"

            case Some(actor) =>
            //AtmosphereClient.broadcast("/fs-moni/live/events", x)
              actor ! x
              log info s"Call $uuid already active"
          }

        case x@CallEnd(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, callUUID,
        callerChannelCreatedTime, callerChannelAnsweredTime, callerChannelHangupTime, freeSWITCHHostname,
        freeSWITCHIPv4, hangupCause, billSec, rtpQualityPerc) =>
          log info s"no uuid $uuid" + x.toString

        case x @ HeartBeat(eventType, eventInfo, upTime, sessionCount, sessionPerSecond, eventDateTimestamp, idleCPU,
        sessionPeakMax, sessionPeakMaxFiveMin, freeSWITCHHostname, freeSWITCHIPv4, uptimeMsec) =>

          AtmosphereClient.broadcast("/fs-moni/live/the-chat",
            JsonMessage(JObject(List(("author", JString("roulis")),
            ("message", JString("lalalalalal")))))
          )

          //AtmosphereClient.broadcast("/live/events", x)
          AtmosphereClient.broadcast("/fs-moni/live/events", ActorsJsonProtocol.heartbeatToJson(x))

         log info "BEAT " + x.toString()
        case x@CallOther(name, uuid) =>
          // log info x.toString
      }
    case GetCalls =>
      val calls = activeCalls.keys.toList
      //log info s"======== $calls"
      // channels / 2 (each call has two channels)
      sender() ! GetCallsResponse(calls.size, calls)

    case x @ GetFailedCalls =>
      log info "--------> ask for failed calls"
      failedCallsActor forward x

    case x @ GetFailedCallsByDate =>
      failedCallsActor forward x

    case x @ GetTotalFailedCalls =>
      failedCallsActor forward x

    case x @ GetCallInfo(callUuid) =>
      (activeCalls get callUuid) match {
        case None =>
          val response = s"Invalid call $callUuid"
          log warning response
          sender() ! response
        case Some(actor) =>
          // get both channels from the next call actor
          log info "----> sending request for call info to actor"
          actor forward x
      }

    case x @ GetChannelInfo(callUuid, channeluuid) =>
      (activeCalls get callUuid) match {
        case None =>
          log warning s"Call $callUuid not found"
        case Some(actor) =>
          actor forward  x
      }

    case Terminated(actor: ActorRef) =>
      val newMap = activeCalls.filter(_._2 != sender())
      context become idle(newMap)

    case _ =>
      log.info("---- I don't know this event")
  }

  def receive: Receive =
    idle(scala.collection.Map.empty[String, ActorRef])
}

