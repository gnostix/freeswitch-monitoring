import javax.servlet.ServletContext

import _root_.akka.actor.{ActorSystem, Props}
import gr.gnostix.freeswitch.actors.CentralMessageRouter
import gr.gnostix.freeswitch.servlets.{LoginServlet, ConfigurationServlet, EslActorApp, WSEslServlet}
import org.scalatra._
import org.scalatra.example.atmosphere.ChatController
import org.slf4j.LoggerFactory

class ScalatraBootstrap extends LifeCycle {
  val system = ActorSystem("esl-sys")
  val myRouter = system.actorOf(Props[CentralMessageRouter], "centralMessageRouter")
  val logger =  LoggerFactory.getLogger(getClass)


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
