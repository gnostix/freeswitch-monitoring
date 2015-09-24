package gr.gnostix.freeswitch.actors

import akka.actor.{ActorRef, Actor, ActorLogging}
import gr.gnostix.freeswitch.actors.ActorsProtocol.{GetFailedCallsAnalysis, GetFailedCallsByDate, GetTotalFailedCalls, GetFailedCalls}
import org.scalatra.atmosphere.AtmosphereClient
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Created by rebel on 17/8/15.
 */

case class TotalFailedCalls(failedCalls: Int)

class FailedCallsActor(wsLiveEventsActor: ActorRef) extends Actor with ActorLogging {

  var failedCalls: List[CallEnd] = List()
  val Tick = "tick"

  def receive: Receive = {
    case x @ CallEnd(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, callUUID,
    callerChannelCreatedTime, callerChannelAnsweredTime, callerChannelHangupTime, freeSWITCHHostname,
    freeSWITCHIPv4, hangupCause, billSec, rtpQualityPerc, otherLegUniqueId) =>

    // the case where we get two FAILED call leg for the same call
    //{"eventName":"FAILED_CALL","fromUser":"0000000000","toUser":"19189898989","callUUID":"8315d80c-c404-4bcb-8612-94edb9863765","freeSWITCHIPv4":"10.143.0.54"}
    //{"eventName":"FAILED_CALL","fromUser":"1000","toUser":"9898989","callUUID":"8315d80c-c404-4bcb-8612-94edb9863765","freeSWITCHIPv4":"10.143.0.54"}

      failedCalls.find(a => a.callUUID == x.callUUID) match {
        case Some(c) =>
          log info "we have this failed call so now we get the second system leg"
          c.fromUser == "0000000000" match {
            case true =>
              failedCalls = failedCalls.filter(ca => ca.callUUID != x.callUUID)
              failedCalls ::= x
            case false =>
          }
        case None =>
          log info "-------> add an extra failed call"
          failedCalls ::= x
          val fCall = FailedCall("FAILED_CALL", x.fromUser, x.toUser, x.callUUID, x.freeSWITCHIPv4)
          wsLiveEventsActor ! fCall
          //wsLiveEventsActor ! ActorsJsonProtocol.failedCallToJson(fCall)
      }

    case x @ GetFailedCallsAnalysis(fromNumberOfDigits, toNumberOfDigits) =>
      sender ! failedCalls.groupBy(x => x.fromUserIP).map{
        case (ip, call) => call.groupBy(pr => pr.fromUser.substring(0,fromNumberOfDigits)).map{
          case (fromUser, call2) => call2.groupBy(_.toUser.substring(0, toNumberOfDigits)).map{
            case (toUser, call3) => (ip, fromUser, toUser, call3.size)
          }
        }
      }

    case x @ GetFailedCalls =>
      //log info "returning the failed calls " + failedCalls
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
