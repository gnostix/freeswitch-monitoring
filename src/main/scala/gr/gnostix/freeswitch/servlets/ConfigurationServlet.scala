package gr.gnostix.freeswitch.servlets

import akka.actor.{ActorRef, ActorSystem}
import gr.gnostix.api.auth.AuthenticationSupport
import gr.gnostix.freeswitch.FreeswitchopStack
import gr.gnostix.freeswitch.actors.ActorsProtocol.{GetEslConnections, EslConnectionData, DelEslConnection}
import gr.gnostix.freeswitch.actors.HeartBeat
import gr.gnostix.freeswitch.actors.ServletProtocol.{ApiReply, ApiReplyData}

// JSON-related libraries
import org.json4s.{DefaultFormats, Formats}

// JSON handling support from Scalatra
import org.scalatra.json._

import org.scalatra.{AsyncResult, CorsSupport, FutureSupport, ScalatraServlet}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration._
import scala.language.postfixOps
/**
 * Created by rebel on 17/9/15.
 */
class ConfigurationServlet(system:ActorSystem, myActor:ActorRef) extends ScalatraServlet
with FutureSupport with JacksonJsonSupport
with CorsSupport with FreeswitchopStack with AuthenticationSupport
{
  implicit val timeout = new Timeout(10 seconds)
  protected implicit def executor: ExecutionContext = system.dispatcher

  before() {
    contentType = formats("json")
    requireLogin()
  }

  options("/*") {
    response.setHeader("Access-Control-Allow-Headers", request.getHeader("Access-Control-Request-Headers"))
  }
  // Sets up automatic case class to JSON output serialization, required by
  // the JValueResult trait.
  protected implicit lazy val jsonFormats: Formats = DefaultFormats

  // /configuration/*

  post("/fs-node/conn-data"){
    log("------------- entering the configuration servlet ---------------- " + parsedBody )
    val eslConnectionData = parsedBody.extract[EslConnectionData]
    val data: Future[ApiReply] = (myActor ? eslConnectionData).mapTo[ApiReply]

    new AsyncResult {
      val is =
        for {
          dt <- data
        } yield dt

    }
  }

  delete("/fs-node/conn-data"){
    val delEslConnection = parsedBody.extract[DelEslConnection]
    val data: Future[ApiReply] = (myActor ? delEslConnection).mapTo[ApiReply]

    new AsyncResult {
      val is =
        for {
          dt <- data
        } yield dt

    }
  }

  get("/fs-node/conn-data"){
    val data: Future[List[EslConnectionData]] = (myActor ? GetEslConnections).mapTo[List[EslConnectionData]]

    new AsyncResult {
      val is =
        for {
          dt <- data
        } yield ApiReplyData(200, "All good ",dt)

    }
  }

  error {
    case t: Throwable => t.printStackTrace()
  }
}
