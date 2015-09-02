package gr.gnostix.freeswitch.servlets

import java.sql.Timestamp

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import gr.gnostix.freeswitch.FreeswitchopStack
import gr.gnostix.freeswitch.actors.ActorsProtocol._
import org.joda.time.DateTime
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.JacksonJsonSupport
import org.scalatra.{CorsSupport, FutureSupport, ScalatraServlet}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

case class Koko(date: DateTime)


class EslActorApp(system:ActorSystem, myActor:ActorRef)
  extends ScalatraServlet with FutureSupport with JacksonJsonSupport with CorsSupport with FreeswitchopStack
{

  implicit val timeout = new Timeout(10 seconds)
  protected implicit def executor: ExecutionContext = system.dispatcher

  // Sets up automatic case class to JSON output serialization, required by
  // the JValueResult trait.
  protected implicit val jsonFormats: Formats = DefaultFormats

  // root path /actors/*

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

  post("/EslConnectionData"){
    val eslConnectionData = parsedBody.extract[EslConnectionData]
    myActor ? eslConnectionData
  }

  get("/GetCalls"){
    myActor ? GetCalls
  }

  get("/GetCompletedCalls"){
    myActor ? GetCompletedCalls
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

  get("/lastHeartbeat"){
    myActor ? GetLastHeartBeat
  }

  get("/allHeartbeats"){
    myActor ? GetAllHeartBeat
  }


  // get basic stats

  get("/stats/GetFailedCallsTimeSeries"){
    myActor ? GetFailedCallsTimeSeries
  }

  get("/stats/GetConcurrentCallsTimeSeries"){
    myActor ? GetConcurrentCallsTimeSeries
  }

  get("/stats/GetBasicAcdTimeSeries"){
    myActor ask GetBasicAcdTimeSeries
  }

}

