package gr.gnostix.freeswitch.actors

import akka.actor.{Actor, ActorLogging}
import gr.gnostix.freeswitch.actors.CallRouter.{GetFailedCallsByDate, GetTotalFailedCalls, GetFailedCalls}
import org.scalatra.atmosphere.AtmosphereClient
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by rebel on 17/8/15.
 */


class FailedCallsActor extends Actor with ActorLogging {

  var failedCalls: List[CallEnd] = List()

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
      sender ! failedCalls.size

    case x @ GetFailedCallsByDate(fromDate, toDate) =>
      sender ! failedCalls.filter(a => a.callerChannelHangupTime.after(fromDate)
                                                && a.callerChannelHangupTime.before(toDate))

      // send a ping message to check the time in the failedCalls Map!!
  }

}
