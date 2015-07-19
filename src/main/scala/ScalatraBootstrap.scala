import gr.gnostix.freeswitch._
import gr.gnostix.freeswitch.actors.{CallRouter}
import gr.gnostix.freeswitch.servlets.{WSEslServlet, EslActorApp, CentralServlet}
import org.scalatra._
import javax.servlet.ServletContext
import _root_.akka.actor.{Props, ActorSystem}
import org.scalatra.example.atmosphere.ChatController

class ScalatraBootstrap extends LifeCycle {
  val system = ActorSystem()
  val myRouter = system.actorOf(Props[CallRouter])
  val myConn = new MyEslConnection(myRouter)

  override def init(context: ServletContext) {
    context.mount(new ChatController, "/*")
    context.mount(new CentralServlet, "/ko/*")
    context.mount(new WSEslServlet, "/live/*")
    context.mount(new EslActorApp(system, myRouter), "/actors/*")
  }

  override def destroy(context:ServletContext) {
    myConn.deinitConnection()
    system.shutdown()
  }
}
