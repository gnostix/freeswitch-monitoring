package gr.gnostix.freeswitch.actors

import akka.actor.{Actor, ActorLogging}
import gr.gnostix.freeswitch.actors.CallRouter.GetLastHeartBeat
import org.scalatra.atmosphere.AtmosphereClient
import scala.concurrent.duration._
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


    case x @ GetLastHeartBeat =>
      sender ! heartBeats.head

    case Tick =>
      heartBeats = getLastsHeartBeats

  }

  context.system.scheduler.schedule(0 milliseconds,
    1000 milliseconds,
    self,
    Tick)

  def getLastsHeartBeats = {
    heartBeats.take(100)
  }

}
