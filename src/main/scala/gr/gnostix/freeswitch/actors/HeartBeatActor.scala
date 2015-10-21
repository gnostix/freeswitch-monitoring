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

import akka.actor.{Props, Actor, ActorLogging, ActorRef}
import gr.gnostix.freeswitch.actors.ActorsProtocol.{GetAllHeartBeat, GetLastHeartBeat, InitializeDashboardHeartBeat}
import gr.gnostix.freeswitch.utilities.HelperFunctions

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Created by rebel on 19/8/15.
 */

object HeartBeatActor {
  def props(wsLiveEventsActor: ActorRef): Props = Props(new HeartBeatActor(wsLiveEventsActor))
}

class HeartBeatActor(wsLiveEventsActor: ActorRef) extends Actor with ActorLogging {

  var heartBeats: List[HeartBeat] = List()

  val Tick = "tick"
  val AvgHeartbeat = "avgHeartbeat"

  def receive: Receive = {
    case x@HeartBeat(eventType, eventInfo, uptimeMsec, concurrentCalls, sessionPerSecond, eventDateTimestamp,
    idleCPU, callsPeakMax, sessionPeakMaxFiveMin, freeSWITCHHostname, freeSWITCHIPv4, upTime, maxAllowedCalls) =>
      heartBeats ::= x
      //AtmosphereClient.broadcast("/fs-moni/live/events", ActorsJsonProtocol.heartbeatToJson(x))
      wsLiveEventsActor ! x
    //wsLiveEventsActor ! ActorsJsonProtocol.heartbeatToJson(x)
    //log info "broadcasted HeartBeat to WS"

    case x@InitializeDashboardHeartBeat =>
      sender ! heartBeats.take(30)

    case x@GetLastHeartBeat =>
      log info "-----------> ask for last heartbeat"
      sender ! heartBeats.headOption.getOrElse(None)

    case x@GetAllHeartBeat =>
      sender ! heartBeats

    case Tick =>
      heartBeats = getLastsHeartBeats
    //log info "Tick coming .. " + heartBeats.size

    /*AvgHeartBeat(eventName: String, uptimeMsec: Long, concurrentCalls: Int, sessionPerSecond: Int,
      eventDateTimestamp: Timestamp, cpuUsage: Double, callsPeakMax: Int, sessionPeakMaxFiveMin: Int,
      maxAllowedCalls: Int)
    */
    case AvgHeartbeat =>
      val averageHeartBeatStats = heartBeats.take(10).groupBy(_.freeSWITCHHostname).map {
        case (x, y) => y.sortWith((leftD, rightD) => leftD.eventDateTimestamp.after(rightD.eventDateTimestamp)).head
      }.toList

      averageHeartBeatStats.size match {
        case x if (x <= 1) => // do nothing becase we already send the HeartBeat for this FS node
        case x if (x > 1) =>
          val avgHeartBeat = AvgHeartBeat("AVG_HEARTBEAT",
            HelperFunctions.roundDouble(averageHeartBeatStats.map(_.uptimeMsec).sum / averageHeartBeatStats.size.toDouble),
            averageHeartBeatStats.map(_.concurrentCalls).sum,
            HelperFunctions.roundDouble(averageHeartBeatStats.map(_.sessionPerSecond).sum / averageHeartBeatStats.size.toDouble),
            new Timestamp(System.currentTimeMillis),
            HelperFunctions.roundDouble(averageHeartBeatStats.map(_.cpuUsage).sum / averageHeartBeatStats.size.toDouble),
            averageHeartBeatStats.sortBy(_.callsPeakMax).last.callsPeakMax,
            averageHeartBeatStats.sortBy(_.sessionPeakMaxFiveMin).last.sessionPeakMaxFiveMin,
            averageHeartBeatStats.map(_.maxAllowedCalls).sum)

          log info s"-----> $avgHeartBeat"

          wsLiveEventsActor ! avgHeartBeat
      }
  }

  context.system.scheduler.schedule(10000 milliseconds,
    1200000 milliseconds,
    self,
    Tick)

  context.system.scheduler.schedule(10000 milliseconds,
    60000 milliseconds,
    self,
    AvgHeartbeat)


  def getLastsHeartBeats = {
    heartBeats.take(1000)
  }

}
