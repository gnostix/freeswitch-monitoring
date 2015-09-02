package gr.gnostix.freeswitch.actors

import java.sql.Timestamp

import akka.actor.{ActorRef, Actor, ActorLogging}
import akka.util.Timeout
import gr.gnostix.freeswitch.actors.ActorsProtocol._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import akka.pattern.ask

import scala.util.{Failure, Success}

/**
 * Created by rebel on 17/8/15.
 */

sealed trait BasicStatsCalls

case class ConcurrentCallsTimeSeries(dateTime: Timestamp, concCallsNum: Int) extends BasicStatsCalls

case class FailedCallsTimeSeries(dateTime: Timestamp, failedCallsNum: Int) extends BasicStatsCalls

case class ACDTimeSeries(dateTime: Timestamp, acd: Int) extends BasicStatsCalls


class BasicStatsActor(callRouterActor: ActorRef, completedCallsActor: ActorRef, wsLiveEventsActor: ActorRef)
  extends Actor with ActorLogging {

  implicit val timeout = Timeout(1 seconds)

  val ConcCalls = "ConcCalls"
  val CurFailedCalls = "CurFailedCalls"
  val Tick = "Tick"
  val ACD = "acd"

  /*
   val callRouterActor = context.actorSelection("/user/centralMessageRouter/callRouter")
   val completedCallsActor = context.actorSelection("/user/centralMessageRouter/completedCallsActor")
   val failedCallsActor = context.actorSelection("/user/centralMessageRouter/callRouter/failedCallsActor")
 */
  var concurrentCalls: List[ConcurrentCallsTimeSeries] = List()
  var failedCalls: List[FailedCallsTimeSeries] = List()
  var basicAcd: List[ACDTimeSeries] = List()
  var totalOfLastValueFailedCalls = 0

  def receive: Receive = {
    case ConcCalls =>
      //log info "ConcCalls tick ..."
      //context.parent ! GetConcurrentCalls
      callRouterActor ! GetConcurrentCalls

    case ConcurrentCallsNum(a) =>
      val calls = ConcurrentCallsTimeSeries(new Timestamp(System.currentTimeMillis), a)
      concurrentCalls ::= calls
      wsLiveEventsActor ! ActorsJsonProtocol.callsTimeSeriesToJson(calls)
      //AtmosphereClient.broadcast("/fs-moni/live/events", ActorsJsonProtocol.callsTimeSeriesToJson(calls))

    //log info "concurrent calls list " + concurrentCalls.toString()

    case CurFailedCalls =>
      //failedCallsActor ! GetTotalFailedCalls
      callRouterActor ! GetTotalFailedCalls

    case x@TotalFailedCalls(a) =>

      // new failed calls = current failed calls minus the previous minute value of the failed calls
      //val y = failedCalls.headOption map (a - _.failedCallsNum) getOrElse 0
      //  log info s"Basic stats actor failed calls a: $a and head: ${failedCalls.headOption map(_.failedCallsNum)} and y: $y"
      val calls = FailedCallsTimeSeries(new Timestamp(System.currentTimeMillis), a - totalOfLastValueFailedCalls)
      totalOfLastValueFailedCalls = a
      failedCalls ::= calls
      wsLiveEventsActor ! ActorsJsonProtocol.callsTimeSeriesToJson(calls)
    //AtmosphereClient.broadcast("/fs-moni/live/events", ActorsJsonProtocol.callsTimeSeriesToJson(calls))


    case Tick =>
      concurrentCalls = getRetentionedConcCalls
      failedCalls = getRetentionedFailedCalls
      basicAcd = getRetentionedAcd

    case ACD =>
      //log info s"-------> basicStats ACD asking for  ACDTimeSeries .."
      val response: Future[List[Int]] = (completedCallsActor ? GetACDLastFor60Seconds).mapTo[List[Int]]
      response.onComplete {
        case Success(a) =>  a match {
          case a @ List() => log warning "------> empty list of acd - reply from completed calls actor"
          case a @ x::_  =>
            log info s"-------> list of ACD ints $a"
            basicAcd ::= ACDTimeSeries(new Timestamp(System.currentTimeMillis), a.sum / a.size)
        }

        case Failure(e) => log info s"-------> BasicStatsActor | ACD response: $e"
      }

    case x@GetFailedCallsTimeSeries =>
      sender ! failedCalls

    case x@GetConcurrentCallsTimeSeries =>
      sender ! concurrentCalls

    case x@GetBasicAcdTimeSeries =>
      sender ! basicAcd

    case x => log info "basic stats actor: I don't know this message " + x.toString
  }

  context.system.scheduler.schedule(10000 milliseconds,
    60000 milliseconds,
    self,
    ConcCalls)

  context.system.scheduler.schedule(10000 milliseconds,
    60000 milliseconds,
    self,
    CurFailedCalls)

  def getRetentionedFailedCalls = {
    // minutes of week 10080 so the entries for one week are also 10080
    failedCalls.take(10080)
  }

  def getRetentionedConcCalls = {
    // minutes of week 10080 so the entries for one week are also 10080
    concurrentCalls.take(10080)
  }

  context.system.scheduler.schedule(10000 milliseconds,
    60000 milliseconds,
    self,
    ACD)

  def getRetentionedAcd = {
    // minutes of week 10080 so the entries for one week are also 10080
    basicAcd.take(10080)
  }
}
