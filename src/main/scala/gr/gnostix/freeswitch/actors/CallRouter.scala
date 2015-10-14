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

import akka.actor.SupervisorStrategy.Restart
import akka.actor._
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import scala.language.postfixOps
import scala.concurrent.duration._

import scala.concurrent.Future


object CallRouter {
  def props(wsLiveEventsActor: ActorRef, completedCallsActor: ActorRef): Props =
    Props(new CallRouter(wsLiveEventsActor, completedCallsActor))
}

class CallRouter(wsLiveEventsActor: ActorRef, completedCallsActor: ActorRef) extends Actor with ActorLogging {

  import gr.gnostix.freeswitch.actors.ActorsProtocol._
  import context.dispatcher

  override val supervisorStrategy =
    OneForOneStrategy() {
      case _ => Restart
    }

  implicit val timeout = Timeout(1 seconds) // needed for `?` below

  // create the FailedCallsActor in advance
  val failedCallsActor = context.actorOf(Props(classOf[FailedCallsActor],wsLiveEventsActor), "failedCallsActor")

  // get reference of CompletedCallsActor
  //val completedCallsActor = context.actorSelection("/user/centralMessageRouter/completedCallsActor")


  def idle(activeCalls: scala.collection.Map[String, ActorRef]): Receive = {

    case x@CallNew(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, toUserIP, callUUID,
    callerChannelCreatedTime, callerChannelAnsweredTime, freeSWITCHHostname, freeSWITCHIPv4, callDirection,
    pdd, ringTimeSec, None, None)
      if callUUID != "_UNKNOWN" =>
      log info x.toString

      (activeCalls get callUUID) match {
        case None =>
//          wsLiveEventsActor ! ActorsJsonProtocol.newCallToJson(x)
          wsLiveEventsActor ! x

          val actor = context.actorOf(Props[CallActor], callUUID)
          actor ! x
          context watch actor

          val newMap = activeCalls updated(callUUID, actor)
          context become idle(newMap)
        case Some(actor) =>
          actor ! x
          log info s"Call $callUUID already active, sending the second channel .."
      }

    case x@CallNew(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, toUserIP, callUUID,
    callerChannelCreatedTime, callerChannelAnsweredTime, freeSWITCHHostname, freeSWITCHIPv4, callDirection,
    pdd, ringTimeSec, None, None) =>
      log info "_UNKNOWN" + x.toString

    case x@CallEnd(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, toUserIP, callUUID,
    callerChannelCreatedTime, callerChannelAnsweredTime, callerChannelHangupTime, freeSWITCHHostname,
    freeSWITCHIPv4, hangupCause, billSec, rtpQualityPerc, otherLegUniqueId, hangupDisposition, callDirection, mos,
    pdd, ringTimeSec, None, None)
      if x.callUUID != "_UNKNOWN" =>
      log info "-----> " + x.toString

      (activeCalls get callUUID) match {
        case None =>

          (activeCalls get otherLegUniqueId) match {
            case None =>
              x.callerChannelAnsweredTime match {
                case None => failedCallsActor ! x
                case Some(a) => log warning s"Call $callUUID doesn't exist! with answered time " + a
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

    case x@CallEnd(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, toUserIP, callUUID,
    callerChannelCreatedTime, callerChannelAnsweredTime, callerChannelHangupTime, freeSWITCHHostname,
    freeSWITCHIPv4, hangupCause, billSec, rtpQualityPerc, otherLegUniqueId, hangupDisposition, callDirection, mos,
    pdd, ringTimeSec, None, None) =>
      log info s"no uuid $uuid" + x.toString

    case GetConcurrentCalls =>
      val calls = activeCalls.keys.toList
      //log info s"======== $calls"
      // channels / 2 (each call has two channels)
      sender() ! GetCallsResponse(calls.size, calls)

    case x@GetTotalConcurrentCalls =>
      // log info "call router GetConcurrentCalls received .."
      sender ! ConcurrentCallsNum(activeCalls.size)

    case x@GetFailedCalls =>
      log info "--------> ask for failed calls"
      failedCallsActor forward x

    case x@GetFailedCallsByDate =>
      failedCallsActor forward x

    case x@GetTotalFailedCalls =>
      failedCallsActor forward x

    case x @ GetFailedCallsAnalysis(fromNumberOfDigits, toNumberOfDigits) =>
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

    case x @ GetConcurrentCallsChannel =>
      val f: List[Future[Option[CallNew]]] = activeCalls.map{
        case (a,y) => (y ? x).mapTo[Option[CallNew]]
      }.toList

      Future.sequence(f) pipeTo sender


    case x @ GetFailedCallsChannel =>
      failedCallsActor forward x

    case CallTerminated(callEnd) =>
      val completedCall = activeCalls.filter(_._2 == sender())

      completedCall.size match {
        case 0 => log error "this call doesn't exist in concurrent calls when completed !!!"
        case _ => completedCallsActor ! CompletedCall(completedCall.head._1, callEnd.callerChannelHangupTime, completedCall.head._2)
      }

      wsLiveEventsActor ! callEnd
      //wsLiveEventsActor ! ActorsJsonProtocol.endCallToJson(callEnd)

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

