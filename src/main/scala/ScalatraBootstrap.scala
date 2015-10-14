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

import javax.servlet.ServletContext

import _root_.akka.actor.{ActorSystem, Props}
import gr.gnostix.freeswitch.actors.CentralMessageRouter
import gr.gnostix.freeswitch.actors.ServletProtocol.{ApiReplyError, ApiReplyData}
import gr.gnostix.freeswitch.servlets.{LoginServlet, ConfigurationServlet, EslActorApp, WSEslServlet}
import gr.gnostix.freeswitch.utilities.FileUtilities
import org.scalatra._
import org.scalatra.example.atmosphere.ChatController
import org.slf4j.LoggerFactory

import scala.collection.SortedMap

class ScalatraBootstrap extends LifeCycle {
  val system = ActorSystem("esl-sys")
  val logger =  LoggerFactory.getLogger(getClass)

  val dialCodes: Map[String, SortedMap[String, String]] = FileUtilities.processResourcesCsvFile() match {
    case x: ApiReplyData =>
      val dialC = x.payload.asInstanceOf[Map[String, SortedMap[String, String]]]
      //logger info s"----> ${x.message} and head list e.g. ${dialC.head} "
      dialC

    case x: ApiReplyError =>
      logger error x.message
      Map("default" -> scala.collection.SortedMap.empty[String, String])
    case _ =>
      logger.debug("we don't understand the reply back from dial codes actor when trying to upload a file")
      Map("default" -> scala.collection.SortedMap.empty[String, String])
  }

  val myRouter = system.actorOf(Props(new CentralMessageRouter(dialCodes)), "centralMessageRouter")

  override def init(context: ServletContext) {
    context.mount(new ConfigurationServlet(system, myRouter)
      , "/configuration/*")
    context.mount(new ChatController, "/the-chat")
    context.mount(new LoginServlet, "/user/*")
    context.mount(new WSEslServlet(system, myRouter), "/fs-moni/live/*")
    context.mount(new EslActorApp(system, myRouter), "/actors/*")
  }

  override def destroy(context:ServletContext) {
    //myConn.deinitConnection()
    system.shutdown()
  }
}
