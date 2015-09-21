package gr.gnostix.freeswitch.servlets

import akka.actor.{ActorRef, ActorSystem}
import gr.gnostix.api.auth.AuthenticationSupport
import gr.gnostix.freeswitch.FreeswitchopStack
import gr.gnostix.freeswitch.actors.ActorsProtocol.{EslConnectionData,DelEslConnection}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.JacksonJsonSupport
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

  options("/*") {
    response.setHeader("Access-Control-Allow-Headers", request.getHeader("Access-Control-Request-Headers"))
  }
  // Sets up automatic case class to JSON output serialization, required by
  // the JValueResult trait.
  protected implicit val jsonFormats: Formats = DefaultFormats

  post("/fs-node/conn-data"){
    log("------------- entering the configuration servlet ---------------- " + parsedBody )
    val eslConnectionData = parsedBody.extract[EslConnectionData]
    myActor ? eslConnectionData
  }

  delete("/fs-node/conn-data"){
    val delEslConnection = parsedBody.extract[DelEslConnection]
    myActor ? delEslConnection
  }

  error {
    case t: Throwable => t.printStackTrace()
  }
}
