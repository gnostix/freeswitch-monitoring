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

import java.util.Date

import _root_.akka.actor.{ActorRef, ActorSystem}
import gr.gnostix.api.auth.AuthenticationSupport
import gr.gnostix.freeswitch.FreeswitchopStack
import gr.gnostix.freeswitch.actors.ActorsProtocol.{RemoveAtmoClientUuid, AddAtmoClientUuid}
import org.atmosphere.cpr.{AtmosphereResource, AtmosphereResourceFactory}
import org.json4s.JsonDSL._
import org.json4s.{DefaultFormats, Formats, _}
import org.scalatra._
import org.scalatra.atmosphere._
import org.scalatra.json.{JValueResult, JacksonJsonSupport}
import org.scalatra.scalate.ScalateSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

/**
 * Created by rebel on 18/7/15.
 */

class WSEslServlet(system:ActorSystem, myActor:ActorRef) extends ScalatraServlet
with ScalateSupport
with JValueResult
with JacksonJsonSupport
with SessionSupport
with AuthenticationSupport
with AtmosphereSupport
with FreeswitchopStack
with CorsSupport {

  protected implicit val jsonFormats: Formats = DefaultFormats

  before() {
    contentType = formats("json")
    requireLogin()
  }

  options("/*") {
    response.setHeader("Access-Control-Allow-Headers", request.getHeader("Access-Control-Request-Headers"))
  }

  get("/test"){
    "test works"
  }


  get("/") {
    contentType="text/html"
    ssp("/login")
  }

  atmosphere("/events") {
    new AtmosphereClient {
      def receive: AtmoReceive = {
        case Connected =>
          //println("params :" + params("events"))
          myActor ! AddAtmoClientUuid(uuid)
          println("Client %s is connected" format uuid)
          //broadcast(("author" -> "Someone") ~ ("message" -> "joined the room") ~ ("time" -> (new Date().getTime.toString )), Everyone)

        case Disconnected(ClientDisconnected, _) =>
          myActor ! RemoveAtmoClientUuid(uuid)
          println("Client %s is disconnected" format uuid)
          //broadcast(("author" -> "Someone") ~ ("message" -> "has left the room") ~ ("time" -> (new Date().getTime.toString )), Everyone)

        case Disconnected(ServerDisconnected, _) =>
          myActor ! RemoveAtmoClientUuid(uuid)
          println("Server disconnected the client %s" format uuid)

        case x @ TextMessage(_) =>
          println("text message received %s" format uuid)
          //broadcast(x.toString())
        //send(("author" -> "system") ~ ("message" -> "Only json is allowed") ~ ("time" -> (new Date().getTime.toString )))

        case JsonMessage(json) =>
          println("------ json " + json.toString)
          println("Got message %s from %s".format((json \ "message").extract[String], (json \ "author").extract[String]))
          val msg = json merge (("time" -> (new Date().getTime().toString)): JValue)
          broadcast(msg) // by default a broadcast is to everyone but self
        //send(msg) // also send to the sender
      }
    }
  }


  error {
    case t: Throwable => t.printStackTrace()
  }
}