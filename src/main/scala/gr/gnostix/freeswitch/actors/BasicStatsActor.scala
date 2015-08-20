package gr.gnostix.freeswitch.actors

import java.sql.Timestamp

import akka.actor.{Actor, ActorLogging}
import gr.gnostix.freeswitch.actors.CallRouter.{ConcurrentCallsNum, GetConcurrentCalls}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by rebel on 17/8/15.
 */

case class ConcurrentCalls(dateTime: Timestamp, callsNum: Int)

class BasicStatsActor extends Actor with ActorLogging {

  val ConcCalls = "ConcCalls"
  val CurFailedCalls = "CurFailedCalls"
  val callRouterActor = context.actorSelection("/user/callRouter")
  var concurrentCalls: List[ConcurrentCalls] = List()


  def receive: Receive = {
    case ConcCalls =>
      //log info "ConcCalls tick ..."
      //context.parent ! GetConcurrentCalls
      callRouterActor ! GetConcurrentCalls

    case ConcurrentCallsNum(a) =>
      concurrentCalls ::=  ConcurrentCalls(new Timestamp(System.currentTimeMillis), a)
      //log info "concurrent calls list " + concurrentCalls.toString()

    case CurFailedCalls =>
    case CallEnd =>
  }

  context.system.scheduler.schedule(0 milliseconds,
    60000 milliseconds,
    self,
    ConcCalls)
}
