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
