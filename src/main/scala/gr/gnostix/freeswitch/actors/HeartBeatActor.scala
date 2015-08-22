package gr.gnostix.freeswitch.actors

import akka.actor.{Actor, ActorLogging}
import gr.gnostix.freeswitch.actors.ActorsProtocol.{GetAllHeartBeat, GetLastHeartBeat}
import org.scalatra.atmosphere.AtmosphereClient
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by rebel on 19/8/15.
 */
class HeartBeatActor extends Actor with ActorLogging {

  var heartBeats: List[HeartBeat] = List()

  val Tick = "tick"

  def receive: Receive = {
    case x @ HeartBeat(eventType, eventInfo, upTime, sessionCount, sessionPerSecond, eventDateTimestamp, idleCPU,
    sessionPeakMax, sessionPeakMaxFiveMin, freeSWITCHHostname, freeSWITCHIPv4, uptimeMsec) =>
      heartBeats ::= x
      AtmosphereClient.broadcast("/fs-moni/live/events", ActorsJsonProtocol.heartbeatToJson(x))
      log info "broadcasted HeartBeat to WS"


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
