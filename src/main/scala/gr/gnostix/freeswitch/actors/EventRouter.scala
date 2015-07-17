package gr.gnostix.freeswitch.actors

import akka.actor.{ActorLogging, Actor}

case class Event(event: String)

class EventRouter extends Actor  with ActorLogging {
  override def receive: Receive = {
    case Event(name) => log.info("################ " +name  + " ##################")
    case _ => log.info("---- I didn't know this event")
  }
}
