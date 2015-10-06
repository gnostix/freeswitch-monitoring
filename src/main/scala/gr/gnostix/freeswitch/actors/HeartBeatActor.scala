package gr.gnostix.freeswitch.actors

import akka.actor.{Actor, ActorLogging, ActorRef}
import gr.gnostix.freeswitch.actors.ActorsProtocol.{GetAllHeartBeat, GetLastHeartBeat, InitializeDashboardHeartBeat}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Created by rebel on 19/8/15.
 */
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
