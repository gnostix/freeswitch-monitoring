package gr.gnostix.freeswitch.actors

import java.sql.Timestamp

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy}
import akka.pattern.{ask, pipe}
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Created by rebel on 27/8/15.
 */

case class HangupActor(hangupTime: Timestamp, callActor: ActorRef)

class CompletedCallsActor extends Actor with ActorLogging {

  import gr.gnostix.freeswitch.actors.ActorsProtocol._
  import context.dispatcher

  val Tick = "tick"
  implicit val timeout = Timeout(1 seconds) // needed for `?` below

  override val supervisorStrategy =
    OneForOneStrategy() {
      case _ => Restart
    }

  def idle(completedCalls: scala.collection.Map[String, HangupActor]): Receive = {
    case CompletedCall(uuid, timeHangup, callActor) =>
      val newMap = completedCalls updated(uuid, HangupActor(timeHangup, callActor))
      //log info s"-----> new call coming on Completed Calls Actor $newMap"
      context become idle(newMap)

    case x @ GetCompletedCallsChannel =>
      val f: List[Future[CallNew]] = completedCalls.map{
        case (a,y) => (y.callActor ? x).mapTo[CallNew]
      }.toList

      Future.sequence(f) pipeTo sender


    case x @ GetACDAndRTPByTime(t) =>
      completedCalls.isEmpty match {
        case true =>
          //log info s"Completed calls Actor GetACDAndRTPForLast60Seconds | NO completedCalls: " + completedCalls
          sender ! List()
        case false =>
          val act = completedCalls.map(x => x._2).filter(s => s.hangupTime.after(t))
          val f: List[Future[CompletedCallStats]] = act.map{
           case a => (a.callActor ? GetACDAndRTP).mapTo[CompletedCallStats]
          }.toList

          //log info s"Completed calls Actor GetACDAndRTPForLast60Seconds , asking all call actors" + Future.sequence(f)
          Future.sequence(f) pipeTo sender
      }

    case x@GetACDAndRTP =>
      completedCalls.isEmpty match {
        case true =>
          //log info s"Completed calls Actor GetACDAndRTPForLast60Seconds | NO completedCalls: " + completedCalls
          sender ! List()
        case false =>
          val f: List[Future[CompletedCallStats]] = completedCalls.map{
            case (x,y) => (y.callActor ? GetACDAndRTP).mapTo[CompletedCallStats]
          }.toList

          //log info s"Completed calls Actor GetACDAndRTPForLast60Seconds , asking all call actors" + Future.sequence(f)
          Future.sequence(f) pipeTo sender
      }

    case x@GetCompletedCalls =>
      val calls = completedCalls.keys.toList
      //log info s"CompletedCallsActor | GetCompletedCalls: $calls"
      // channels / 2 (each call has two channels)
      sender() ! GetCallsResponse(calls.size, calls)

    case x@GetCompletedCallMinutes =>
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
          actor.callActor forward x
      }

    case x@GetChannelInfo(callUuid, channeluuid) =>
      (completedCalls get callUuid) match {
        case None =>
          val response = s"Invalid call $callUuid"
          log warning response
          sender() ! response

        case Some(actor) =>
          actor.callActor forward x
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
    idle(scala.collection.Map.empty[String, HangupActor])
}
