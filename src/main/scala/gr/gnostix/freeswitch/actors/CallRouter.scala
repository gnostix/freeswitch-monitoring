package gr.gnostix.freeswitch.actors

import akka.actor.SupervisorStrategy.Restart
import org.json4s.DefaultReaders.JValueReader
import org.json4s.JsonAST.JValue
import org.json4s.jackson.Json
import org.scalatra.atmosphere.{JsonMessage, AtmosphereClient, TextMessage}

import scala.collection.JavaConverters._
import akka.actor.{OneForOneStrategy, ActorRef, ActorLogging, Actor}
import org.freeswitch.esl.client.transport.event.EslEvent
import scala.concurrent.ExecutionContext.Implicits.global
// JSON-related libraries
import org.json4s.{DefaultFormats, Formats}

// JSON handling support from Scalatra
import org.scalatra.json._

sealed trait EventType {
  def name: String
  def uuid: String
}

sealed trait CallEventType extends EventType {
  def fromUser: String
  def toUser: String
}
case class CallNew(uuid: String,fromUser: String,toUser: String) extends CallEventType {
  val name = "CHANNEL_STATE"
}
case class CallEnd(uuid: String,fromUser: String,toUser: String) extends CallEventType {
  val name = "CHANNEL_STATE"
}
case class CallOther(name: String, uuid: String) extends EventType



object CallRouter {
  protected implicit lazy val jsonFormats: Formats = DefaultFormats

  sealed trait RouterProtocol
  sealed trait RouterRequest extends RouterProtocol
  sealed trait RouterResponse extends RouterProtocol
  case class Event(headers: scala.collection.Map[String, String]) extends RouterRequest
  case object GetCalls extends RouterRequest
  case class GetCallsResponse(activeCalls: List[String]) extends RouterResponse
  case class GetCallInfo(uuid: String) extends RouterRequest

  object Event {
    def apply(event: EslEvent): Event = Event(event.getEventHeaders.asScala)
    def apply(): Event = Event(scala.collection.Map.empty[String, String])
  }

  def mkEvent(event: EslEvent): Event = Event(event)
}



class CallRouter extends Actor with ActorLogging {
  import CallRouter._

  override val supervisorStrategy =
    OneForOneStrategy() {
      case _ => Restart
    }

  def getCallEventType(headers: scala.collection.Map[String, String]): EventType = {
    // Channel-Call-UUID is unique for call but since we have one event for every channel it comes twice
    // Unique-ID is unique per channel. In every call we have two channels
    val uuid = headers get "Unique-ID" getOrElse "_UNKNOWN"
    (headers get "Event-Name") match {
      case Some("CHANNEL_ANSWER") => CallNew(uuid,headers get "Caller-Caller-ID-Number" getOrElse "_UNKNOWN",
        headers get "Caller-Destination-Number" getOrElse "_UNKNOWN")
      case Some("CHANNEL_HANGUP_COMPLETE") => CallEnd(uuid,headers get "Caller-Caller-ID-Number" getOrElse "_UNKNOWN",
        headers get "Caller-Destination-Number" getOrElse "_UNKNOWN")
      case Some(x) => CallOther(x, uuid)
      case None => CallOther("_UNKNOWN", uuid)
    }
  }

  def idle(activeCalls: scala.collection.Map[String, ActorRef]): Receive = {
    case Event(headers) =>

      //object TextCallHeaders extends TextMessage(headers.mkString)
      //object JsonCallHeaders extends JsonMessage(Json(headers))

      //AtmosphereClient.broadcast("/live/events", TextCallHeaders)

      getCallEventType(headers) match {
        case x @ CallNew(uuid,fromUser,toUser) if uuid != "_UNKNOWN" =>
          log info x.toString

          (activeCalls get uuid) match {
            case None =>
              object TextCallInfo extends TextMessage(x.toString)
              AtmosphereClient.broadcast("/live/events", TextCallInfo)

              val actor = context actorOf CallActor.props(uuid)
              val newMap = activeCalls updated (uuid, actor)
              context become idle(newMap)
            case Some(actor) =>
              log warning s"Call $uuid already active"
          }
        case x @ CallNew(uuid,fromUser,toUser) =>
          log info x.toString
        case x @ CallEnd(uuid,fromUser,toUser) if uuid != "_UNKNOWN" =>
          log info "-----> " + x.toString

          (activeCalls get uuid) match {
            case None =>
              log warning s"Call $uuid not found"
            case Some(actor) =>
              log info "call removed from activeCalls"
              object TextCallInfo extends TextMessage(x.toString)
              AtmosphereClient.broadcast("/live/events", TextCallInfo)

              context stop actor
              val newMap = activeCalls - uuid
              context become idle(newMap)
          }
        case x @ CallEnd(uuid,fromUser,toUser) =>
          log info s"no uuid $uuid" + x.toString
        case x @ CallOther(name, uuid) =>
          //log info x.toString
      }
    case GetCalls =>
      val calls = activeCalls.keys.toList
      log info s"======== $calls"
      sender() ! GetCallsResponse(calls)
    case x @ GetCallInfo(uuid) =>
      (activeCalls get uuid) match {
        case None =>
          val response = s"Invalid call $uuid"
          log warning response
          sender() ! response
        case Some(actor) =>
          actor forward x
      }
    case _ =>
      log.info("---- I don't know this event")
  }

  def receive: Receive =
    idle(scala.collection.Map.empty[String, ActorRef])
}
