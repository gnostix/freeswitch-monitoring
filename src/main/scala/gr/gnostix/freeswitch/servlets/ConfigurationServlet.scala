package gr.gnostix.freeswitch.servlets

import java.io.File

import _root_.akka.actor.{ActorRef, ActorSystem}
import gr.gnostix.api.auth.AuthenticationSupport
import gr.gnostix.freeswitch.FreeswitchopStack
import gr.gnostix.freeswitch.actors.ActorsProtocol.{GetEslConnections, EslConnectionData, DelEslConnection}
import gr.gnostix.freeswitch.actors.HeartBeat
import gr.gnostix.freeswitch.actors.ServletProtocol.{ApiReply, ApiReplyData}
import org.scalatra.servlet.{SizeConstraintExceededException, FileItem, MultipartConfig, FileUploadSupport}

// JSON-related libraries
import org.json4s.{DefaultFormats, Formats}

// JSON handling support from Scalatra
import org.scalatra.json._

import org.scalatra._
import _root_.akka.pattern.ask
import _root_.akka.util.Timeout
import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration._
import scala.language.postfixOps
/**
 * Created by rebel on 17/9/15.
 */
class ConfigurationServlet(system:ActorSystem, myActor:ActorRef) extends ScalatraServlet
with FutureSupport with JacksonJsonSupport with FileUploadSupport
with CorsSupport with FreeswitchopStack with AuthenticationSupport
{
  implicit val timeout = new Timeout(10 seconds)
  protected implicit def executor: ExecutionContext = system.dispatcher

  before() {
    contentType = formats("json")
//    requireLogin()
  }
  configureMultipartHandling(MultipartConfig(maxFileSize = Some(5*1024*1024)))

  options("/*") {
    response.setHeader("Access-Control-Allow-Headers", request.getHeader("Access-Control-Request-Headers"))
  }
  // Sets up automatic case class to JSON output serialization, required by
  // the JValueResult trait.
  protected implicit lazy val jsonFormats: Formats = DefaultFormats

  // /configuration/*

  post("/dialcodes"){
    log info " ----> entering dialcodes ..."
    fileParams.get("filename") match {
      case Some(file) =>
        /*Ok(file.get(), Map(
          "Content-Type"        -> (file.contentType.getOrElse("application/octet-stream")),
          "Content-Disposition" -> ("attachment; filename=\"" + file.name + "\"")
        ))*/
        processFile(file)


      case None =>
        ApiReply(400, "No file selected!")
    }

  }

  def processFile(file: FileItem): Map[String, Long] = {
    var dialCodesMap = scala.collection.Map.empty[String, Long]
    val csv = io.Source.createBufferedSource(file.getInputStream)
    //val fileString =  new String(file.get)//..map(x => log info s" -----> file size: ${x}")
    val lines = csv.getLines()
    //log info "---lines size " + lines
    for (l <- csv.getLines()){
      //log info "---lines data " + l
      val cols = l.split(";").map(_.trim)
      dialCodesMap += (cols(0) -> cols(1).toLong)
    }
    dialCodesMap
  }

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
    log info "----> get esl connections ---"
    val data: Future[List[EslConnectionData]] = (myActor ? GetEslConnections).mapTo[List[EslConnectionData]]

    new AsyncResult {
      val is =
        for {
          dt <- data
        } yield ApiReplyData(200, "All good ",dt)

    }
  }

  error {
    case e: SizeConstraintExceededException => RequestEntityTooLarge("very large file..")
    case t: Throwable => t.printStackTrace()
  }
}
