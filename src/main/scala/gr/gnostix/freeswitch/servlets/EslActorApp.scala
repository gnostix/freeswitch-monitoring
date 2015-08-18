package gr.gnostix.freeswitch.servlets

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import gr.gnostix.freeswitch.actors.CallRouter._
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.JacksonJsonSupport
import org.scalatra.{Accepted, FutureSupport, ScalatraServlet}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class EslActorApp(system:ActorSystem, myActor:ActorRef)
  extends ScalatraServlet with FutureSupport with JacksonJsonSupport {

  implicit val timeout = new Timeout(2 seconds)
  protected implicit def executor: ExecutionContext = system.dispatcher

  // Sets up automatic case class to JSON output serialization, required by
  // the JValueResult trait.
  protected implicit val jsonFormats: Formats = DefaultFormats

  before() {
    contentType = formats("json")
  }

  // You'll see the output from this in the browser.
  get("/ask") {
    "Do stuff and give me an answer"
  }

  get("/GetCalls"){
    myActor ? GetCalls
  }

  get("/GetFailedCalls"){
    myActor ? GetFailedCalls
  }

  get("/GetTotalFailedCalls"){
    myActor ? GetTotalFailedCalls
  }

  get("/call/:callUuid/channel/:channelUuid"){
    myActor ? GetChannelInfo(params("callUuid"), params("channelUuid"))
  }

  get("/call/:callid"){
    myActor ? GetCallInfo(params("callid"))
  }



}

