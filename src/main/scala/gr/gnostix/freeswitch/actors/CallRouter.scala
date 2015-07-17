package gr.gnostix.freeswitch.actors

import akka.actor.SupervisorStrategy.Restart

import scala.collection.JavaConverters._
import akka.actor.{OneForOneStrategy, ActorRef, ActorLogging, Actor}
import org.freeswitch.esl.client.transport.event.EslEvent

sealed trait CallEventType {
  def name: String
  def uuid: String
}
case class CallNew(uuid: String) extends CallEventType {
  val name = "CHANNEL_STATE"
}
case class CallEnd(uuid: String) extends CallEventType {
  val name = "CHANNEL_STATE"
}
case class CallOther(name: String, uuid: String) extends CallEventType

object CallRouter {
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

  def getCallEventType(headers: scala.collection.Map[String, String]): CallEventType = {
    val uuid = headers get "Core-UUID" getOrElse "_UNKNOWN"
    (headers get "Event-Name") match {
      case Some("CHANNEL_ANSWER") => CallNew(uuid)
      case Some("CHANNEL_HANGUP_COMPLETE") => CallEnd(uuid)
      case Some(x) => CallOther(x, uuid)
      case None => CallOther("_UNKNOWN", uuid)
    }
  }

  def idle(activeCalls: scala.collection.Map[String, ActorRef]): Receive = {
    case Event(headers) =>
      getCallEventType(headers) match {
        case x @ CallNew(uuid) if uuid != "_UNKNOWN" =>
          log info x.toString
          (activeCalls get uuid) match {
            case None =>
              val actor = context actorOf CallActor.props(uuid)
              val newMap = activeCalls updated (uuid, actor)
              context become idle(newMap)
            case Some(actor) =>
              log warning s"Call $uuid already active"
          }
        case x @ CallNew(uuid) =>
          log warning x.toString
        case x @ CallEnd(uuid) if uuid != "_UNKNOWN" =>
          log info x.toString
          (activeCalls get uuid) match {
            case None =>
              log warning s"Call $uuid not found"
            case Some(actor) =>
              context stop actor
              val newMap = activeCalls - uuid
              context become idle(newMap)
          }
        case x @ CallEnd(uuid) =>
          log warning x.toString
        case x @ CallOther(name, uuid) =>
          log info x.toString
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
