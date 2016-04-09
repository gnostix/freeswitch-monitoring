/*
 * Copyright (c) 2015 Alexandros Pappas p_alx hotmail com
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package gr.gnostix.freeswitch.actors

import java.sql.Timestamp

import akka.actor.{Props, ActorRef, Actor, ActorLogging}
import akka.util.Timeout
import gr.gnostix.freeswitch.actors.ActorsProtocol._
import gr.gnostix.freeswitch.model.CompletedCallStatsByIP
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import akka.pattern.ask

import scala.util.{Failure, Success}

/**
 * Created by rebel on 17/8/15.
 */

sealed trait BasicStatsCalls {
  def eventName: String
}

case class SwitchIpHostname(ip: String, host: String)

case class BasicStatsTimeSeries(eventName: String, dateTime: Timestamp, concCallsNum: Int, failedCallsNum: Int,
                                acd: Double, asr: Double, rtpQualityAvg: Double, ipAddress: Option[String], hostname: Option[String]) extends BasicStatsCalls

case class CustomStatsPerCustomerTimeSeries(dateTime: Timestamp, concCallsNum: Int, failedCallsNum: Int, acd: Int, asr: Double,
                                            rtpQualityAvg: Double, ip: List[String], cliPrefixRegEx: Option[List[String]])


sealed trait CustomJobs

//object CustomJob extends CustomJobs

case class StatsPerCustomer(ip: List[String], cliPrefixRegEx: Option[List[String]], desc: String) extends CustomJobs

case class StatsPerProvider(ip: List[String], cliPrefixRegEx: Option[List[String]], desc: String) extends CustomJobs


object BasicStatsActor {
  def props(callRouterActor: ActorRef, completedCallsActor: ActorRef, wsLiveEventsActor: ActorRef): Props =
    Props(new BasicStatsActor(callRouterActor, completedCallsActor, wsLiveEventsActor))
}

class BasicStatsActor(callRouterActor: ActorRef, completedCallsActor: ActorRef, wsLiveEventsActor: ActorRef)
  extends Actor with ActorLogging {

  implicit val timeout = Timeout(1 seconds)

  val BASIC_STATS = "BASIC_STATS"
  val AVG_BASIC_STATS = "AVG_BASIC_STATS"
  val BasicStatsTick = "BasicStatsTick"
  val Tick = "Tick"
  var lastBasicStatsTickTime = new Timestamp(System.currentTimeMillis)
  var basicStats: List[BasicStatsTimeSeries] = List()
  var customStatsPerCust: List[CustomStatsPerCustomerTimeSeries] = List()
  var customJobs: List[StatsPerCustomer] = List()

  def receive: Receive = {

    case BasicStatsTick =>
      val conCallsT: Future[List[Option[CallNew]]] = (callRouterActor ? GetConcurrentCallsChannel).mapTo[List[Option[CallNew]]]
      val failedCallsT: Future[List[CallEnd]] = (callRouterActor ? GetFailedCallsChannelByTime(lastBasicStatsTickTime)).mapTo[List[CallEnd]]
      val completedCallsStatsT: Future[List[CompletedCallStatsByIP]] =
        (completedCallsActor ? GetACDAndRTPByTime(lastBasicStatsTickTime)).mapTo[List[CompletedCallStatsByIP]]

      // acd = sum of minutes devided by the number of calls
      // asr = the % of devide the successfully completed calls by the total number of calls and then multiply by 100
      val response = for {
        r1 <- conCallsT
        r2 <- failedCallsT
        r3 <- completedCallsStatsT
      } yield {
          val switchUniqueIPs = getSwitchUniqueIPs(r1, r2, r3)
          val basicStatsSeriesToSend = extractBasicTimeSeries(r1, r2, r3, switchUniqueIPs)

          basicStatsSeriesToSend

        }

      response.onComplete {
        case Success(x) =>
          //log info "------> response from BasicStatsTick"
          x.asInstanceOf[List[BasicStatsTimeSeries]].map {
            bStats =>
              log info s"---> Bstats: $bStats"
              wsLiveEventsActor ! bStats
          }

          //wsLiveEventsActor ! ActorsJsonProtocol.caseClassToJsonMessage(x._1)
          basicStats :::= x.asInstanceOf[List[BasicStatsTimeSeries]]
          log info s"---------> the asr data completed Calls: "

        case Failure(e) => log warning "BasicStatsTick failed in response " + e.getMessage
      }


    case Tick =>
      basicStats = getRetentionedBasicStats

    case x@GetBasicStatsTimeSeries =>
      sender ! basicStats

    case x@InitializeDashboardBasicStats =>
      sender ! basicStats.take(30)

    case x => log info "basic stats actor: I don't know this message " + x.toString
  }


  def getSwitchUniqueIPs(concCalls: List[Option[CallNew]], failedCalls: List[CallEnd],
                         compCalls: List[CompletedCallStatsByIP]): List[SwitchIpHostname] = {

    val hosts = concCalls.flatten.groupBy(_.freeSWITCHIPv4).map(ip => SwitchIpHostname(ip._2.head.freeSWITCHIPv4, ip._2.head.freeSWITCHHostname)).toList :::
      failedCalls.groupBy(_.freeSWITCHIPv4).map(ip => SwitchIpHostname(ip._2.head.freeSWITCHIPv4, ip._2.head.freeSWITCHHostname)).toList :::
      compCalls.groupBy(_.ipAddress).map(ip => SwitchIpHostname(ip._2.head.ipAddress, ip._2.head.hostname)).toList

    val uniqueHosts = hosts.groupBy(_.ip).map {
      case (ip, uH) => uH.head
    }.toList

    log info s"the hosts: $uniqueHosts"

    uniqueHosts
  }


  def extractBasicTimeSeries(concCalls: List[Option[CallNew]], failedCalls: List[CallEnd], compCalls: List[CompletedCallStatsByIP],
                             switchUniqueIPs: List[SwitchIpHostname]): List[BasicStatsTimeSeries] = {
    switchUniqueIPs.size match {
      case x if (x == 0) => List() // no switch available so do nothing
      case x if (x == 1) => // for one switch just send the plain data..
        // when we checked last about these data
        val bsStats = getBasicStatsSeriesByIp(concCalls.flatten, failedCalls, compCalls, switchUniqueIPs.headOption)
        List(bsStats)

      case x if (x > 1) => {
        // for many switches configured we do the following steps..
        // first send for each  Switch IP address
        val basicStatsPerIP = switchUniqueIPs.map {
          c => {
            val concCallsByIP = concCalls.flatten.filter(_.freeSWITCHIPv4 == c)
            val failedCallsByIP = failedCalls.filter(_.freeSWITCHIPv4 == c)
            val compCallsByIP = compCalls.filter(_.ipAddress == c)

            // get the basic stats time serie for this ip
            getBasicStatsSeriesByIp(concCallsByIP, failedCallsByIP, compCallsByIP, Some(c))
          }
        }

        // here send the average for all Switches
        val avgBS = getBasicStatsSeriesByIp(concCalls.flatten, failedCalls, compCalls, None)
        // when we checked last about these data

        basicStatsPerIP :+ avgBS
      }

    }
  }


  def getBasicStatsSeriesByIp(concCallsByIP: List[CallNew], failedCallsByIP: List[CallEnd], compCalls: List[CompletedCallStatsByIP],
                              switchUniqueIPHost: Option[SwitchIpHostname]): BasicStatsTimeSeries = {

    val compCallsByIPSorted = compCalls.sortWith { (leftE, rightE) => leftE.callerChannelHangupTime.after(rightE.callerChannelHangupTime) }

    val asr = compCallsByIPSorted.size match {
      case 0 => 0
      case _ =>
        log info s" ------> switchUniqueIPHost ${switchUniqueIPHost.headOption.get}"
        failedCallsByIP.size match {
          case 0 => 100
          case _ => BigDecimal(compCallsByIPSorted.size.toDouble / (compCallsByIPSorted.size + failedCallsByIP.size) * 100).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
        }
    }

    val acd = compCallsByIPSorted.size match {
      case 0 => 0
      case _ =>
        // update last check time for Stats
        lastBasicStatsTickTime = compCallsByIPSorted.head.callerChannelHangupTime
        BigDecimal((compCallsByIPSorted.map(x => x.acd).sum / compCallsByIPSorted.size) / 60.toDouble).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble //seconds to minutes
    }

    val rtpQ = compCallsByIPSorted.size match {
      case 0 => 0
      case _ =>
        log info s" ------> acd r3Sorted sum ${compCallsByIPSorted.map(x => x.acd).sum / compCallsByIPSorted.size} and size ${compCallsByIPSorted.size} and failedalls size ${failedCallsByIP}"
        compCallsByIPSorted.map(x => x.rtpQuality).sum / compCallsByIPSorted.size.toDouble
    }

    switchUniqueIPHost match {
      case Some(dt) => BasicStatsTimeSeries(BASIC_STATS, new Timestamp(System.currentTimeMillis), concCallsByIP.size, failedCallsByIP.size, acd, asr, rtpQ, Some(dt.ip), Some(dt.host))
      case None => BasicStatsTimeSeries(AVG_BASIC_STATS, new Timestamp(System.currentTimeMillis), concCallsByIP.size, failedCallsByIP.size, acd, asr, rtpQ, None, None)
    }


  }


  context.system.scheduler.schedule(10000 milliseconds,
    20000 milliseconds,
    self,
    BasicStatsTick)


  def getRetentionedBasicStats = {
    // minutes of week 10080 so the entries for one week are also 10080
    basicStats.take(10080)
  }
}
