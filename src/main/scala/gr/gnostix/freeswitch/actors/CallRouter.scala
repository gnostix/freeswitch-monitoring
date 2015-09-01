package gr.gnostix.freeswitch.actors

import akka.actor.SupervisorStrategy.Restart
import akka.actor._
import gr.gnostix.freeswitch.actors.ActorsProtocol.CompletedCall


class CallRouter(wsLiveEventsActor: ActorRef, completedCallsActor: ActorRef) extends Actor with ActorLogging {

  import gr.gnostix.freeswitch.actors.ActorsProtocol._

  override val supervisorStrategy =
    OneForOneStrategy() {
      case _ => Restart
    }

  // create the FailedCallsActor in advance
  val failedCallsActor = context.actorOf(Props[FailedCallsActor], "failedCallsActor")

  // get reference of CompletedCallsActor
  //val completedCallsActor = context.actorSelection("/user/centralMessageRouter/completedCallsActor")


  def idle(activeCalls: scala.collection.Map[String, ActorRef]): Receive = {

    case x@CallNew(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, callUUID,
    callerChannelCreatedTime, callerChannelAnsweredTime, freeSWITCHHostname, freeSWITCHIPv4)
      if callUUID != "_UNKNOWN" =>
      log info x.toString

      (activeCalls get callUUID) match {
        case None =>
          wsLiveEventsActor ! ActorsJsonProtocol.newCallToJson(x)

          val actor = context.actorOf(Props[CallActor], callUUID)
          actor ! x
          context watch actor

          val newMap = activeCalls updated(callUUID, actor)
          context become idle(newMap)
        case Some(actor) =>
          actor ! x
          log info s"Call $callUUID already active, sending the second channel .."
      }

    case x@CallNew(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, callUUID,
    callerChannelCreatedTime, callerChannelAnsweredTime, freeSWITCHHostname, freeSWITCHIPv4) =>
      log info "_UNKNOWN" + x.toString

    case x@CallEnd(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, callUUID,
    callerChannelCreatedTime, callerChannelAnsweredTime, callerChannelHangupTime, freeSWITCHHostname,
    freeSWITCHIPv4, hangupCause, billSec, rtpQualityPerc, otherLegUniqueId) if callUUID != "_UNKNOWN" =>
      log info "-----> " + x.toString

      (activeCalls get callUUID) match {
        case None =>

          (activeCalls get otherLegUniqueId) match {
            case None =>
              x.callerChannelAnsweredTime match {
                case None => failedCallsActor ! x
                case Some(a) => log info s"Call $callUUID doesn't exist! with answered time " + a
              }
              log info s"Call $callUUID doesn't exist!"

            case Some(actor) =>
              actor ! x
              log info s"Call otherLegUniqueId $otherLegUniqueId already active"

          }

        case Some(actor) =>
          actor ! x
          log info s"Call $callUUID already active"
      }

    case x@CallEnd(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, callUUID,
    callerChannelCreatedTime, callerChannelAnsweredTime, callerChannelHangupTime, freeSWITCHHostname,
    freeSWITCHIPv4, hangupCause, billSec, rtpQualityPerc, otherLegUniqueId) =>
      log info s"no uuid $uuid" + x.toString

    case GetCalls =>
      val calls = activeCalls.keys.toList
      //log info s"======== $calls"
      // channels / 2 (each call has two channels)
      sender() ! GetCallsResponse(calls.size, calls)

    case x@GetConcurrentCalls =>
      // log info "call router GetConcurrentCalls received .."
      sender ! ConcurrentCallsNum(activeCalls.size)

    case x@GetFailedCalls =>
      log info "--------> ask for failed calls"
      failedCallsActor forward x

    case x@GetFailedCallsByDate =>
      failedCallsActor forward x

    case x@GetTotalFailedCalls =>
      failedCallsActor forward x

    case x@GetCallInfo(callUuid) =>
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

    case x@GetChannelInfo(callUuid, channeluuid) =>
      (activeCalls get callUuid) match {
        case None =>
          val response = s"Invalid call $callUuid"
          log warning response
          sender() ! response

        case Some(actor) =>
          actor forward x
      }

    case CallTerminated(callEnd) =>
      val completedCall = activeCalls.filter(_._2 == sender())
      completedCallsActor ! CompletedCall(completedCall.head._1, completedCall.head._2)

      wsLiveEventsActor ! ActorsJsonProtocol.endCallToJson(callEnd)

      val newMap = activeCalls.filter(_._2 != sender())
      context become idle(newMap)

    /*
        case Terminated(actor: ActorRef) =>
          val completedCall = activeCalls.filter(_._2 == sender())
          completedCallsActor ! completedCall

          val newMap = activeCalls.filter(_._2 != sender())
          context become idle(newMap)
    */

    case _ =>
      log.info("---- I don't know this event")
  }

  def receive: Receive =
    idle(scala.collection.Map.empty[String, ActorRef])
}

