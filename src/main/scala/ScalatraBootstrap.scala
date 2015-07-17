import gr.gnostix.freeswitch._
import gr.gnostix.freeswitch.actors.EventRouter
import gr.gnostix.freeswitch.servlets.{EslActorApp, CentralServlet}
import org.scalatra._
import javax.servlet.ServletContext
import _root_.akka.actor.{Props, ActorSystem}

class ScalatraBootstrap extends LifeCycle {
  val system = ActorSystem()
  val myRouter = system.actorOf(Props[EventRouter])
  val myConn = new MyEslConnection(system)

  override def init(context: ServletContext) {
    context.mount(new CentralServlet, "/*")
    context.mount(new EslActorApp(system, myRouter), "/actors/*")
  }

  override def destroy(context:ServletContext) {
    myConn.deinitConnection()
    system.shutdown()
  }
}
