package gr.gnostix.freeswitch.servlets

import java.sql.Timestamp
import java.util.Date

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import gr.gnostix.freeswitch.FreeswitchopStack
import gr.gnostix.freeswitch.actors.CallRouter._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.JacksonJsonSupport
import org.scalatra.{CorsSupport, Accepted, FutureSupport, ScalatraServlet}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

case class Koko(date: DateTime)


class EslActorApp(system:ActorSystem, myActor:ActorRef)
  extends ScalatraServlet with FutureSupport with JacksonJsonSupport with CorsSupport with FreeswitchopStack
{

  implicit val timeout = new Timeout(2 seconds)
  protected implicit def executor: ExecutionContext = system.dispatcher

  // Sets up automatic case class to JSON output serialization, required by
  // the JValueResult trait.
  protected implicit val jsonFormats: Formats = DefaultFormats


  before() {
    contentType = formats("json")
  }

  options("/*") {
    response.setHeader("Access-Control-Allow-Headers", request.getHeader("Access-Control-Request-Headers"))
  }

  get("/koko"){
    Koko(new DateTime())
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

  get("/GetFailedCallsByDate/:fromDate/:toDate"){
/*    val fromDate: DateTime = DateTime.parse(params("fromDate"),
      DateTimeFormat.forPattern("dd-MM-yyyy HH:mm:ss"))
    logger info(s"---->   parsed date ---> ${fromDate}    ")

    val toDate: DateTime = DateTime.parse(params("toDate"),
      DateTimeFormat.forPattern("dd-MM-yyyy HH:mm:ss"))
    logger info(s"---->   parsed date ---> ${toDate}    ")*/

    val from = Timestamp.valueOf(params("fromDate"))
    val to = Timestamp.valueOf(params("toDate"))

    myActor ? GetFailedCallsByDate(from, to)
  }

  get("/call/:callUuid/channel/:channelUuid"){
    myActor ? GetChannelInfo(params("callUuid"), params("channelUuid"))
  }

  get("/call/:callid"){
    myActor ? GetCallInfo(params("callid"))
  }



}

