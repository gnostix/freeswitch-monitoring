package gr.gnostix.freeswitch.servlets

import org.scalatra._
import scalate.ScalateSupport
import org.scalatra.atmosphere._
import org.scalatra.servlet.AsyncSupport
import org.scalatra.json.{JValueResult, JacksonJsonSupport}
import org.json4s._
import JsonDSL._
import java.util.Date
import java.text.SimpleDateFormat
import org.fusesource.scalate.Template

import scala.concurrent._
import ExecutionContext.Implicits.global

/**
 * Created by rebel on 18/7/15.
 */

class WSEslServlet extends ScalatraServlet
with ScalateSupport
with JValueResult
with JacksonJsonSupport with SessionSupport
with AtmosphereSupport {

  protected implicit val jsonFormats: Formats = DefaultFormats

  atmosphere("/events") {
    log("---------------> atmosphere /live/events")
    new AtmosphereClient {
      def receive: AtmoReceive = {
        case Connected =>
          println("Client %s is connected" format uuid)
          broadcast(("author" -> "Someone") ~ ("message" -> "joined the room") ~ ("time" -> (new Date().getTime.toString)), Everyone)

        case Disconnected(ClientDisconnected, _) =>
          broadcast(("author" -> "Someone") ~ ("message" -> "has left the room") ~ ("time" -> (new Date().getTime.toString)), Everyone)

        case Disconnected(ServerDisconnected, _) =>
          println("Server disconnected the client %s" format uuid)

        case _: TextMessage =>
          broadcast(("author" -> "system") ~ ("message" -> "Only json is allowed") ~ ("time" -> (new Date().getTime.toString)))

        case JsonMessage(json) =>
          println("-----> message received on events " + json.toString)
          //val msg = json merge (("time" -> (new Date().getTime().toString)): JValue)
          broadcast(json) // by default a broadcast is to everyone but self
        //  send(msg) // also send to the sender
      }
    }
  }

  error {
    case t: Throwable => t.printStackTrace()
  }
}