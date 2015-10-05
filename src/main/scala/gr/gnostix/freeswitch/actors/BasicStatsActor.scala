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

case class BasicStatsTimeSeries(eventName: String, dateTime: Timestamp, concCallsNum: Int, failedCallsNum: Int,
                                acd: Double, asr: Double, rtpQualityAvg: Double) extends BasicStatsCalls

case class CustomStatsPerCustomerTimeSeries(dateTime: Timestamp, concCallsNum: Int, failedCallsNum: Int, acd: Int, asr: Double,
                                            rtpQualityAvg: Double, ip: List[String], cliPrefixRegEx: Option[List[String]])


sealed trait CustomJobs

//object CustomJob extends CustomJobs

case class StatsPerCustomer(ip: List[String], cliPrefixRegEx: Option[List[String]], desc: String) extends CustomJobs
case class StatsPerProvider(ip: List[String], cliPrefixRegEx: Option[List[String]], desc: String) extends CustomJobs


class BasicStatsActor(callRouterActor: ActorRef, completedCallsActor: ActorRef, wsLiveEventsActor: ActorRef)
  extends Actor with ActorLogging {

  implicit val timeout = Timeout(1 seconds)

  val BASIC_STATS = "BASIC_STATS"
  val BasicStatsTick = "BasicStatsTick"
  val Tick = "Tick"
  var lastBasicStatsTickTime = new Timestamp(System.currentTimeMillis)
  var basicStats: List[BasicStatsTimeSeries] = List()
  var customStatsPerCust: List[CustomStatsPerCustomerTimeSeries] = List()
  var customJobs: List[StatsPerCustomer] = List()
  var totalOfLastValueFailedCalls = 0

  def receive: Receive = {

    case BasicStatsTick =>
      val conCallsT: Future[ConcurrentCallsNum] = (callRouterActor ? GetTotalConcurrentCalls).mapTo[ConcurrentCallsNum]
      val failedCallsT: Future[TotalFailedCalls] = (callRouterActor ? GetTotalFailedCalls).mapTo[TotalFailedCalls]
      val completedCallsStatsT: Future[List[CompletedCallStats]] =
        (completedCallsActor ? GetACDAndRTPByTime(lastBasicStatsTickTime)).mapTo[List[CompletedCallStats]]

      // acd = sum of minutes devided by the number of calls
      // asr = the % of devide the successfully completed calls by the total number of calls and then multiply by 100
      val response = for {
        r1 <- conCallsT
        r2 <- failedCallsT
        r3 <- completedCallsStatsT
      } yield {
          r3.isEmpty match {
            case true =>
              //log info s"------> response from BasicStatsTick failed calls ${r2.failedCalls} - totalOfLastValueFailedCalls ${totalOfLastValueFailedCalls}"
              (BasicStatsTimeSeries(BASIC_STATS, new Timestamp(System.currentTimeMillis), r1.calls, r2.failedCalls - totalOfLastValueFailedCalls, 0, 0, 0),r2.failedCalls)
            case false => {
              log info s"------> response from BasicStatsTick failed calls ${r2.failedCalls - totalOfLastValueFailedCalls} - completed calls ${r3.size}"
              val r3Sorted = r3.sortWith { (leftE, rightE) => leftE.callerChannelHangupTime.before(rightE.callerChannelHangupTime) }
              val asr = BigDecimal(r3Sorted.size.toDouble / (r3Sorted.size + (r2.failedCalls - totalOfLastValueFailedCalls)) * 100).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
                log info s" ------> acd r3Sorted sum ${r3Sorted.map(x => x.acd).sum / r3Sorted.size} and size ${r3Sorted.size}"
              val acd = BigDecimal((r3Sorted.map(x => x.acd).sum / r3Sorted.size) / 60.toDouble).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble //seconds to minutes
              val rtpQ = r3Sorted.map(x => x.rtpQuality).sum / r3Sorted.size.toDouble
              lastBasicStatsTickTime = r3Sorted.reverse.head.callerChannelHangupTime

              (BasicStatsTimeSeries(BASIC_STATS, new Timestamp(System.currentTimeMillis), r1.calls, r2.failedCalls - totalOfLastValueFailedCalls, acd, asr, rtpQ),r2.failedCalls)
            }

          }

        }
      //${r3.size} failed calls: ${r2.failedCalls}"

      response.onComplete {
        case Success(x) =>
          //log info "------> response from BasicStatsTick"
          wsLiveEventsActor ! x._1
          //wsLiveEventsActor ! ActorsJsonProtocol.caseClassToJsonMessage(x._1)
          basicStats ::= x._1
          totalOfLastValueFailedCalls = x._2
          log info s"---------> the asr data completed Calls: "

        case Failure(e) => log warning "BasicStatsTick failed in response " + e.getMessage
      }

//    case x@StatsPerCustomer(_, _) =>
//      customJobs ::= x

    case Tick =>
      basicStats = getRetentionedBasicStats

    case x@GetBasicStatsTimeSeries =>
      sender ! basicStats

    case x@InitializeDashboard =>
      basicStats.take(50).map{e => wsLiveEventsActor ! e}

    case x => log info "basic stats actor: I don't know this message " + x.toString
  }

  context.system.scheduler.schedule(10000 milliseconds,
    10000 milliseconds,
    self,
    BasicStatsTick)


  def getRetentionedBasicStats = {
    // minutes of week 10080 so the entries for one week are also 10080
    basicStats.take(10080)
  }
}
