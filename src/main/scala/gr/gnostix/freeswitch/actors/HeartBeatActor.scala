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

import akka.actor.{Props, Actor, ActorLogging, ActorRef}
import gr.gnostix.freeswitch.actors.ActorsProtocol.{GetAllHeartBeat, GetLastHeartBeat, InitializeDashboardHeartBeat}

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

  def receive: Receive = {
    case x @ HeartBeat(eventType, eventInfo, uptimeMsec, sessionCount, sessionPerSecond, eventDateTimestamp, cpuUsage,
    sessionPeakMax, sessionPeakMaxFiveMin, freeSWITCHHostname, freeSWITCHIPv4, upTime) =>
      heartBeats ::= x
      //AtmosphereClient.broadcast("/fs-moni/live/events", ActorsJsonProtocol.heartbeatToJson(x))
      wsLiveEventsActor ! x
      //wsLiveEventsActor ! ActorsJsonProtocol.heartbeatToJson(x)
      //log info "broadcasted HeartBeat to WS"

    case x@InitializeDashboardHeartBeat =>
      sender ! heartBeats.take(30)

    case x @ GetLastHeartBeat =>
      log info "-----------> ask for last heartbeat"
      sender ! heartBeats.headOption.getOrElse(None)

    case x @ GetAllHeartBeat =>
      sender ! heartBeats

    case Tick =>
      heartBeats = getLastsHeartBeats
      //log info "Tick coming .. " + heartBeats.size

  }

  context.system.scheduler.schedule(10000 milliseconds,
    1200000 milliseconds,
    self,
    Tick)

  def getLastsHeartBeats = {
    heartBeats.take(1000)
  }

}
