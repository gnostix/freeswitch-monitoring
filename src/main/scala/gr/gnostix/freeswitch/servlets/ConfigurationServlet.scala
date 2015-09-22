package gr.gnostix.freeswitch.servlets

import akka.actor.{ActorRef, ActorSystem}
import gr.gnostix.api.auth.AuthenticationSupport
import gr.gnostix.freeswitch.FreeswitchopStack
import gr.gnostix.freeswitch.actors.ActorsProtocol.{GetEslConnections, EslConnectionData, DelEslConnection}
// JSON-related libraries
import org.json4s.{DefaultFormats, Formats}

// JSON handling support from Scalatra
import org.scalatra.json._

import org.scalatra.{CorsSupport, FutureSupport, ScalatraServlet}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.ExecutionContext
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
    myActor ? eslConnectionData
  }

  delete("/fs-node/conn-data"){
    val delEslConnection = parsedBody.extract[DelEslConnection]
    myActor ? delEslConnection
  }

  get("/fs-node/conn-data"){
    myActor ? GetEslConnections
  }

  error {
    case t: Throwable => t.printStackTrace()
  }
}
