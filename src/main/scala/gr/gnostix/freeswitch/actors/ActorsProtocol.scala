package gr.gnostix.freeswitch.actors

import java.sql.Timestamp
import akka.actor.ActorRef

import scala.collection.JavaConverters._

import org.freeswitch.esl.client.transport.event.EslEvent

import scala.collection.SortedMap

/**
 * Created by rebel on 23/8/15.
 */


object ActorsProtocol {

  sealed trait RouterProtocol

  sealed trait RouterRequest extends RouterProtocol

  sealed trait RouterResponse extends RouterProtocol

  case class Event(headers: scala.collection.Map[String, String]) extends RouterRequest

  case object InitializeDashboardHeartBeat extends RouterRequest

  case object InitializeDashboardBasicStats extends RouterRequest

  case object GetConcurrentCalls extends RouterRequest

  case object GetCompletedCalls extends RouterRequest

  case object GetTotalConcurrentCalls extends RouterRequest

  case class ConcurrentCallsNum(calls: Int) extends RouterRequest

  case object GetTotalFailedCalls extends RouterRequest

  case object GetFailedCalls extends RouterRequest

  case class GetFailedCallsAnalysis(fromNumberOfDigits: Int, toNumberOfDigits: Int) extends RouterRequest

  case class GetFailedCallsByDate(from: Timestamp, to: Timestamp) extends RouterRequest

  case class GetCallsResponse(totalCalls: Int, activeCallsUUID: List[String]) extends RouterResponse

  case class GetCallInfo(uuid: String) extends RouterRequest

  case class GetChannelInfo(callUuid: String, channelUuid: String) extends RouterRequest

  case object GetConcurrentCallsChannel extends RouterRequest

  case object GetFailedCallsChannel extends RouterRequest

  case object GetCompletedCallsChannel extends RouterRequest


  case object GetLastHeartBeat extends RouterRequest

  case object GetAllHeartBeat extends RouterRequest

  //case object GetFailedCallsTimeSeries extends RouterRequest

  case object GetBasicStatsTimeSeries extends RouterRequest

  //case object GetConcurrentCallsTimeSeries extends RouterRequest

  //case object GetBasicAcdTimeSeries extends RouterRequest

  case object GetCompletedCallMinutes extends RouterRequest

  case object GetEslConnections extends RouterRequest

  case class EslConnectionData(ip: String, port: Int, password: String) extends RouterRequest

  case class DelEslConnection(ip: String) extends RouterRequest

  case class ShutdownEslConnection(ip: String) extends RouterRequest

  case class CompletedCall(uuid: String, hangupTime: Timestamp, callActor: ActorRef) extends RouterProtocol

  case class CallTerminated(callEnd: CallEnd) extends RouterProtocol

  case object GetACDAndRTP extends RouterRequest

  case object GetACDAndRTPByCountry extends RouterRequest

  case class GetACDAndRTPByTime(lastCheck: Timestamp) extends RouterRequest

  case class AcdData(acd: Double) extends RouterResponse

  case class GetNumberDialCode(number: String) extends RouterRequest

  case class NumberDialCodeCountry(prefix: Option[String], country: Option[String]) extends RouterResponse

  case class AddDialCodeList(fileName: String, dialCodes: SortedMap[String, String]) extends RouterRequest

  case class DelDialCodeList(fileName: String) extends RouterRequest

  case class GetDialCodeList(fileName: String) extends RouterRequest

  case object GetAllDialCodeList extends RouterRequest

  case class AddAtmoClientUuid(uuid: String)

  case class RemoveAtmoClientUuid(uuid: String)

  object Event {
    def apply(event: EslEvent): Event = Event(event.getEventHeaders.asScala)

    def apply(): Event = Event(scala.collection.Map.empty[String, String])
  }

  def mkEvent(event: EslEvent): Event = Event(event)
}

object ServletProtocol {
  sealed trait ApiProtocol
  sealed trait ApiRequest extends ApiProtocol
  sealed trait ApiResponse extends ApiProtocol

  case class ApiReply(status: Int, message: String) extends  ApiResponse
  case class ApiReplyError(status: Int, message: String) extends  ApiResponse
  case class ApiReplyData(status: Int, message: String, payload: Any) extends  ApiResponse

}