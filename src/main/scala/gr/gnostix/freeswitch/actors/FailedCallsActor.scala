package gr.gnostix.freeswitch.actors

import akka.actor.{Actor, ActorLogging}
import gr.gnostix.freeswitch.actors.CallRouter.{GetFailedCallsByDate, GetTotalFailedCalls, GetFailedCalls}
import org.scalatra.atmosphere.AtmosphereClient
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
 * Created by rebel on 17/8/15.
 */

case class TotalFailedCalls(failedCalls: Int)

class FailedCallsActor extends Actor with ActorLogging {

  var failedCalls: List[CallEnd] = List()
  val Tick = "tick"

  def receive: Receive = {
    case x @ CallEnd(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, callUUID,
    callerChannelCreatedTime, callerChannelAnsweredTime, callerChannelHangupTime, freeSWITCHHostname,
    freeSWITCHIPv4, hangupCause, billSec, rtpQualityPerc) =>
      log info "-------> add an extra failed call"
      failedCalls ::= x
      val fCall = FailedCall("FAILED_CALL", x.fromUser, x.toUser, x.callUUID, x.freeSWITCHIPv4)
      AtmosphereClient.broadcast("/fs-moni/live/events", ActorsJsonProtocol.failedCallToJson(fCall))

    case x @ GetFailedCalls =>
      log info "returning the failed calls " + failedCalls
      sender ! failedCalls

    case x @ GetTotalFailedCalls =>
      //log info "returning the failed calls size " + failedCalls.size
      sender ! TotalFailedCalls(failedCalls.size)

    case x @ GetFailedCallsByDate(fromDate, toDate) =>
      sender ! failedCalls.filter(a => a.callerChannelHangupTime.after(fromDate)
                                                && a.callerChannelHangupTime.before(toDate))

    case Tick =>
      failedCalls = getLastsFailedCalls

  }

  context.system.scheduler.schedule(10000 milliseconds,
    1200000 milliseconds,
    self,
    Tick)

  def getLastsFailedCalls = {
    failedCalls.take(5000)
  }

}
