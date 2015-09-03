package gr.gnostix.freeswitch.actors

import akka.actor.{Actor, ActorLogging}
import org.scalatra.atmosphere.{AtmosphereClient, OutboundMessage}
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatra.atmosphere.AtmosphereClient

/**
 * Created by rebel on 29/8/15.
 */
class WSLiveEventsActor extends Actor with ActorLogging {

  def receive: Receive = {

    case x: OutboundMessage =>
      //AtmosphereClient.broadcast("/fs-moni/live/events", x)

    case _ => log warning "WSLiveEventsActor | Unknown Message .."
  }

}
