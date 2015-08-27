import gr.gnostix.freeswitch._
import gr.gnostix.freeswitch.actors.{CentralMessageRouter, EslEventRouter, CallRouter}
import gr.gnostix.freeswitch.servlets.{WSEslServlet, EslActorApp, CentralServlet}
import org.scalatra._
import javax.servlet.ServletContext
import _root_.akka.actor.{Props, ActorSystem}
import org.scalatra.example.atmosphere.ChatController
import org.slf4j.LoggerFactory

class ScalatraBootstrap extends LifeCycle {
  val system = ActorSystem("esl-sys")
  val myRouter = system.actorOf(Props[CentralMessageRouter], "centralMessageRouter")
 // val myConn = new MyEslConnection(myRouter, system)
  val logger =  LoggerFactory.getLogger(getClass)


  override def init(context: ServletContext) {
    context.mount(new ChatController, "/fs-moni/live/*")
    context.mount(new CentralServlet, "/ko/*")
    //context.mount(new WSEslServlet, "/live/*")
    context.mount(new EslActorApp(system, myRouter), "/actors/*")
  }

  override def destroy(context:ServletContext) {
    //myConn.deinitConnection()
    system.shutdown()
  }
}
