/*
 * Copyright (c) 2015 Alexandros Pappas p_alx hotmail com
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package gr.gnostix.freeswitch.servlets

import _root_.akka.actor.{ActorRef, ActorSystem}
import gr.gnostix.api.auth.AuthenticationSupport
import gr.gnostix.freeswitch.FreeswitchopStack
import gr.gnostix.freeswitch.actors.ActorsProtocol._
import gr.gnostix.freeswitch.actors.ServletProtocol.{ApiReply, ApiReplyData, ApiReplyError}
import gr.gnostix.freeswitch.utilities.FileUtilities
import org.scalatra.servlet.{FileUploadSupport, MultipartConfig, SizeConstraintExceededException}

import scala.collection.SortedMap

// JSON-related libraries

import org.json4s.{DefaultFormats, Formats}

// JSON handling support from Scalatra

import _root_.akka.pattern.ask
import _root_.akka.util.Timeout
import org.scalatra._
import org.scalatra.json._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

/**
 * Created by rebel on 17/9/15.
 */
class ConfigurationServlet(system: ActorSystem, myActor: ActorRef) extends ScalatraServlet
with FutureSupport with JacksonJsonSupport with FileUploadSupport
with CorsSupport with FreeswitchopStack with AuthenticationSupport
with ContentEncodingSupport {
  implicit val timeout = new Timeout(10 seconds)

  protected implicit def executor: ExecutionContext = system.dispatcher

  before() {
    contentType = formats("json")
   // requireLogin()
  }
  configureMultipartHandling(MultipartConfig(maxFileSize = Some(5 * 1024 * 1024)))

  options("/*") {
    response.setHeader("Access-Control-Allow-Headers", request.getHeader("Access-Control-Request-Headers"))
  }
  // Sets up automatic case class to JSON output serialization, required by
  // the JValueResult trait.
  protected implicit lazy val jsonFormats: Formats = DefaultFormats

  // /configuration/*

  get("/dialcodes") {
    val data: Future[List[AllDialCodes]] = (myActor ? GetAllDialCodeList).mapTo[List[AllDialCodes]]

    new AsyncResult {
      val is =
        for {
          dt <- data
        } yield  { ApiReplyData(200, "all good", dt) }

    }
  }

  get("/dialcodes/:filename") {
    val fileName = params("filename")
    val data: Future[Option[SortedMap[String, String]]] = (myActor ? GetDialCodeList(fileName)).mapTo[Option[SortedMap[String, String]]]

    new AsyncResult {
      val is =
        for {
          dt <- data
        } yield {
          dt match {
            case Some(dialC) => ApiReplyData(200, "all good", dialC)
            case _ => response.sendError(400, s"No dial codes list with file name: $fileName")
          }
        }

    }
  }

  post("/dialcodes") {
    fileParams.get("filename") match {
      case Some(file) =>
        log info s" ----> post entering dialcodes, filename: ${file.getName}, size:  ${file.getSize}"
        FileUtilities.processCsvFileItem(file.getName, file) match {
          case x: ApiReplyError =>
            response.sendError(x.status, x.message)

          case x: ApiReplyData =>
            // push the data to the actor
            myActor ! AddDialCodeList(file.getName, x.payload.asInstanceOf[Map[String, SortedMap[String, String]]].head._2)
            ApiReplyData(200, x.message, x.payload.asInstanceOf[Map[String, SortedMap[String, String]]].last._2.size)

          case x =>
            log error s"we dont understand this reply "
            response.sendError(400, "General error")
        }


      case None =>
        response.sendError(400, "No file selected!")
    }

  }

  delete("/dialcodes/:filename") {
    // remove dialcodes from this file name
    val fileName = params("filename")

    val data: Future[ApiReply] = (myActor ? DelDialCodeList(fileName)).mapTo[ApiReply]

    new AsyncResult {
      val is =
        for {
          dt <- data
        } yield {
          dt.status match {
            case 400 => response.sendError(400, dt.message)
            case _ => dt
          }
        }

    }

  }

  post("/fs-node/conn-data") {
    log("------------- entering the configuration servlet ---------------- " + parsedBody)
    val eslConnectionData = parsedBody.extract[EslConnectionData]
    val data: Future[ApiReply] = (myActor ? eslConnectionData).mapTo[ApiReply]

    new AsyncResult {
      val is =
        for {
          dt <- data
        } yield {
          dt.status match {
            case 400 => response.sendError(400, dt.message)
            case _ => dt
          }
        }

    }
  }

  delete("/fs-node/conn-data") {
    val delEslConnection = parsedBody.extract[DelEslConnection]
    val data: Future[ApiReply] = (myActor ? delEslConnection).mapTo[ApiReply]

    new AsyncResult {
      val is =
        for {
          dt <- data
        } yield {
          dt.status match {
            case 400 => response.sendError(400, dt.message)
            case _ => dt
          }
        }

    }
  }

  get("/fs-node/conn-data") {
    log info "----> get esl connections ---"
    val data: Future[List[EslConnectionData]] = (myActor ? GetEslConnections).mapTo[List[EslConnectionData]]

    new AsyncResult {
      val is =
        for {
          dt <- data
        } yield ApiReplyData(200, "All good ", dt)

    }
  }

  error {
    case e: SizeConstraintExceededException => RequestEntityTooLarge("very large file..")
    case t: Throwable => t.printStackTrace()
  }
}
