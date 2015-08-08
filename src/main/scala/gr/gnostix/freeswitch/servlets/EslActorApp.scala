package gr.gnostix.freeswitch.servlets

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import gr.gnostix.freeswitch.actors.CallRouter.{GetChannelInfo, GetCallInfo, GetCalls}
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
    myActor ? "Do stuff and give me an answer"
  }

  get("/GetCalls"){
    myActor ? GetCalls
  }

  get("/channel/:callid"){
    myActor ? GetCallInfo(params("callid"))
  }

  get("/call/:callid"){
    myActor ? GetChannelInfo(params("callid"))
  }

  // You'll see the output from this in your terminal.
  get("/tell") {
    myActor ! "Hey, you know what?"
    Accepted()
  }

}

