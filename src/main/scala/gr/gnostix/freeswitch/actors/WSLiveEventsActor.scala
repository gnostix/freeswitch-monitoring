package gr.gnostix.freeswitch.actors

import java.util.Date

import akka.actor.{Actor, ActorLogging}
import org.json4s.JsonDSL._
import org.json4s._
import org.scalatra.atmosphere._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

/**
 * Created by rebel on 29/8/15.
 */
class WSLiveEventsActor extends Actor with ActorLogging {

  implicit val atmoC = new AtmosphereClient {
    override def receive: AtmoReceive = ???
  }

  def receive: Receive = {

    case x: OutboundMessage =>
      AtmosphereClient.broadcast("/fs-moni/live/events", x)
      //broadcast(x, SkipSelf)
    // not good , in every message we open a new client!!!! I need to find a way to broadcast messages
    // only to specific clients
      /*new AtmosphereClient {
        def receive: AtmoReceive = {
/*
          case Connected =>
            println("Client %s is connected" format uuid)
            broadcast(("author" -> "Someone") ~ ("message" -> "joined the room") ~ ("time" -> (new Date().getTime.toString)), Everyone)

          case Disconnected(ClientDisconnected, _) =>
            broadcast(("author" -> "Someone") ~ ("message" -> "has left the room") ~ ("time" -> (new Date().getTime.toString)), Everyone)

          case Disconnected(ServerDisconnected, _) =>
            println("Server disconnected the client %s" format uuid)

          case _: TextMessage =>
            broadcast(("author" -> "system") ~ ("message" -> "Only json is allowed") ~ ("time" -> (new Date().getTime.toString)))
*/

          case JsonMessage(json) =>
            println("-----> message received on events " + json.toString)
            //val msg = json merge (("time" -> (new Date().getTime().toString)): JValue)
            broadcast(x, SkipSelf) // by default a broadcast is to everyone but self
            //send(x) // also send to the sender
        }*/
    //  }
    case _ => log warning "WSLiveEventsActor | Unknown Message .."
  }

}
