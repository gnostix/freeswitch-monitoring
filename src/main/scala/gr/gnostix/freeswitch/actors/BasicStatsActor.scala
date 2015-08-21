package gr.gnostix.freeswitch.actors

import java.sql.Timestamp

import akka.actor.{Actor, ActorLogging}
import gr.gnostix.freeswitch.actors.CallRouter._
import org.scalatra.atmosphere.AtmosphereClient
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by rebel on 17/8/15.
 */

sealed trait BasicStatsCalls
case class ConcurrentCallsTimeSeries(dateTime: Timestamp, concCallsNum: Int) extends BasicStatsCalls
case class FailedCallsTimeSeries(dateTime: Timestamp, failedCallsNum: Int) extends BasicStatsCalls


class BasicStatsActor extends Actor with ActorLogging {

  val ConcCalls = "ConcCalls"
  val CurFailedCalls = "CurFailedCalls"
  val Tick = "Tick"
  val callRouterActor = context.actorSelection("/user/callRouter")
  val failedCallsActor = context.actorSelection("/user/callRouter/failedCallsActor")

  var concurrentCalls: List[ConcurrentCallsTimeSeries] = List()
  var failedCalls: List[FailedCallsTimeSeries] = List()


  def receive: Receive = {
    case ConcCalls =>
      //log info "ConcCalls tick ..."
      //context.parent ! GetConcurrentCalls
      callRouterActor ! GetConcurrentCalls

    case ConcurrentCallsNum(a) =>
      val calls = ConcurrentCallsTimeSeries(new Timestamp(System.currentTimeMillis), a)
      concurrentCalls ::=  calls
      AtmosphereClient.broadcast("/fs-moni/live/events", ActorsJsonProtocol.callsTimeSeriesToJson(calls))

    //log info "concurrent calls list " + concurrentCalls.toString()

    case CurFailedCalls =>
      failedCallsActor ! GetTotalFailedCalls

    case x @ TotalFailedCalls(a) =>
    // new failed calls = current failed calls minus the previous minute value of the failed calls
    val y = failedCalls.headOption map (a - _.failedCallsNum) getOrElse a
    val calls = FailedCallsTimeSeries(new Timestamp(System.currentTimeMillis), y)
    failedCalls ::= calls
    AtmosphereClient.broadcast("/fs-moni/live/events", ActorsJsonProtocol.callsTimeSeriesToJson(calls))


    case Tick =>
      concurrentCalls = getRetentionedConcCalls
      failedCalls = getRetentionedFailedCalls

    case x @ GetFailedCallsTimeSeries =>
      sender ! failedCalls

    case x @ GetConcurrentCallsTimeSeries =>
      sender ! concurrentCalls

    case x => log info "basic stats actor: I don't know this message "+ x.toString
  }

  context.system.scheduler.schedule(60000 milliseconds,
    60000 milliseconds,
    self,
    ConcCalls)

  context.system.scheduler.schedule(60000 milliseconds,
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


}
