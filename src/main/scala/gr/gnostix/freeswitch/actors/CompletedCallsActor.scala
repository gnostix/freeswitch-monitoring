package gr.gnostix.freeswitch.actors

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{OneForOneStrategy, ActorRef, Actor, ActorLogging}
import scala.collection.Map
import scala.collection.immutable.Map
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by rebel on 27/8/15.
 */
class CompletedCallsActor extends Actor with ActorLogging {

  import gr.gnostix.freeswitch.actors.ActorsProtocol._

  val Tick = "tick"

  override val supervisorStrategy =
    OneForOneStrategy() {
      case _ => Restart
    }

  def idle(completedCalls: scala.collection.Map[String, ActorRef]): Receive = {
    case CompletedCall(uuid,callActor) =>
      val newMap = completedCalls  updated(uuid, callActor)
      log info s"-----> new call coming on Completed Calls Actor $newMap"
      context become idle(newMap)

    case x @ GetCompletedCalls =>
      val calls = completedCalls.keys.toList
      //log info s"======== $calls"
      // channels / 2 (each call has two channels)
      sender() ! GetCallsResponse(calls.size, calls)

    case x @ GetCompletedCallMinutes =>
      // ask all callActors about the call minutes of each call and sum them and send them back

    case x@GetCallInfo(callUuid) =>
      (completedCalls get callUuid) match {
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
      (completedCalls get callUuid) match {
        case None =>
          val response = s"Invalid call $callUuid"
          log warning response
          sender() ! response

        case Some(actor) =>
          actor forward x
      }

    case Tick =>
      val newMap = completedCalls.take(10080)
      // I should make sure here that we take the newest calls!
      context become idle(newMap)

    case x =>
      log.info("---- I don't know this event " + x)
  }

  context.system.scheduler.schedule(60000 milliseconds,
    60000 milliseconds,
    self,
    Tick)


  def receive: Receive =
    idle(scala.collection.Map.empty[String, ActorRef])
}
