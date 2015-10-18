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


import java.sql.Timestamp

import _root_.akka.actor.{ActorRef, ActorSystem}
import _root_.akka.pattern.ask
import _root_.akka.util.Timeout
import gr.gnostix.api.auth.AuthenticationSupport
import gr.gnostix.freeswitch.FreeswitchopStack
import gr.gnostix.freeswitch.actors.ActorsProtocol._
import gr.gnostix.freeswitch.actors.{BasicStatsTimeSeries, HeartBeat, CallEnd, CallNew}
import gr.gnostix.freeswitch.actors.ServletProtocol.{ApiReply, ApiReplyData}
import gr.gnostix.freeswitch.model.CompletedCallStatsByCountry
import gr.gnostix.freeswitch.utilities.HelperFunctions
import org.joda.time.DateTime
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.JacksonJsonSupport
import org.scalatra._

import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}


class EslActorApp(system:ActorSystem, myActor:ActorRef)
  extends ScalatraServlet with FutureSupport with JacksonJsonSupport
  with CorsSupport with FreeswitchopStack with AuthenticationSupport
  with GZipSupport
{

  implicit val timeout = new Timeout(10 seconds)
  protected implicit def executor: ExecutionContext = system.dispatcher

  // Sets up automatic case class to JSON output serialization, required by
  // the JValueResult trait.
  protected implicit val jsonFormats: Formats = DefaultFormats

  // root path /actors/*

  before() {
    contentType = formats("json")
   // requireLogin()
  }

  options("/*") {
    response.setHeader("Access-Control-Allow-Headers", request.getHeader("Access-Control-Request-Headers"))
  }


  // You'll see the output from this in the browser.
  get("/ask") {
    "Do stuff and give me an answer"
  }

  get("/initialize/dashboard/heartbeat"){
    val data: Future[List[HeartBeat]] = (myActor ? InitializeDashboardHeartBeat).mapTo[List[HeartBeat]]

    new AsyncResult {
      val is =
        for {
          dt <- data
        } yield ApiReplyData(200,"all good", dt)

    }
  }

  get("/initialize/dashboard/basicstats"){
    val data: Future[List[BasicStatsTimeSeries]] = (myActor ? InitializeDashboardBasicStats).mapTo[List[BasicStatsTimeSeries]]

    new AsyncResult {
      val is =
        for {
          dt <- data
        } yield ApiReplyData(200,"all good", dt)

    }
  }


  get("/GetConcurrentCalls"){
    myActor ? GetConcurrentCalls
  }

  get("/GetTotalConcurrentCalls"){
    myActor ? GetTotalConcurrentCalls
  }

  get("/concurrent/calls/details"){
    val data: Future[List[Option[CallNew]]] = (myActor ? GetConcurrentCallsChannel).mapTo[List[Option[CallNew]]]

    new AsyncResult {
      val is =
        for {
          dt <- data
        } yield ApiReplyData(200,"all good", dt)

    }
  }

/*  get("/GetCompletedCalls"){
    myActor ? GetCompletedCalls
  }*/

  get("/failed/calls/details"){
    val data: Future[List[CallEnd]] = (myActor ? GetFailedCallsChannel).mapTo[List[CallEnd]]

    new AsyncResult {
      val is =
        for {
          dt <- data
        } yield ApiReplyData(200,"all good", dt)

    }
  }

  get("/completed/calls/details"){
    val data: Future[List[CallEnd]] = (myActor ? GetCompletedCallsChannel).mapTo[List[CallEnd]]

    new AsyncResult {
      val is =
        for {
          dt <- data
        } yield ApiReplyData(200,"all good", dt)

    }
  }

  //GetACDAndRTPByCountry
  get("/completed/calls/country/acdrtp"){
    val data: Future[List[Option[CompletedCallStatsByCountry]]] =
      (myActor ? GetBillSecAndRTPByCountry).mapTo[List[Option[CompletedCallStatsByCountry]]]

    new AsyncResult {
      val is =
        for {
          dt <- data
        } yield ApiReplyData(200,"all good", HelperFunctions.sortAcdByCountry(dt))

    }
  }

  get("/completed/calls/country/asr"){
    val fCalls: Future[List[CallEnd]] = (myActor ? GetFailedCalls).mapTo[List[CallEnd]]
    val cCalls: Future[List[Option[CompletedCallStatsByCountry]]] = (myActor ? GetBillSecAndRTPByCountry).mapTo[List[Option[CompletedCallStatsByCountry]]]

    new AsyncResult {
      val is =
        for {
          fc <- fCalls
          cc <- cCalls
        } yield ApiReplyData(200,"all good", HelperFunctions.getAsrByCountry(cc, fc))

    }
  }


  get("/GetFailedCalls"){
    myActor ? GetFailedCalls
  }

  get("/GetTotalFailedCalls"){
    myActor ? GetTotalFailedCalls
  }

  post("/get-failed-calls-analysis"){
    val failedCallsAnalysis = parsedBody.extract[GetFailedCallsAnalysis]
    myActor ? failedCallsAnalysis
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

  get("/stats/GetBasicStatsTimeSeries"){
    myActor ? GetBasicStatsTimeSeries
  }
/*
  get("/stats/GetConcurrentCallsTimeSeries"){
    myActor ? GetConcurrentCallsTimeSeries
  }

  get("/stats/GetBasicAcdTimeSeries"){
    myActor ask GetBasicAcdTimeSeries
  }*/

  error {
    case t: Throwable => t.printStackTrace()
  }
}

