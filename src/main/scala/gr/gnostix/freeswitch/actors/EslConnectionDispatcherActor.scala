package gr.gnostix.freeswitch.actors

import akka.actor._
import gr.gnostix.freeswitch.actors.ServletProtocol.ApiReply
import gr.gnostix.freeswitch.{EslConnection}
import gr.gnostix.freeswitch.actors.ActorsProtocol.{ShutdownEslConnection, EslConnectionData}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by rebel on 27/8/15.
 */
class EslConnectionDispatcherActor(wSLiveEventsActor: ActorRef) extends Actor with ActorLogging {

  val Tick = "tick"
  var actorConnections: scala.collection.Map[String, EslConnection] = Map()

  def idle(connections: scala.collection.Map[String, EslConnection]): Receive = {

    case x @ EslConnectionData(ip, port, password) =>
      connections get x.ip match {
        case None =>
          val eslEventRouter = context.actorOf(Props[EslEventRouter], x.ip)
          val eslConn = new EslConnection(eslEventRouter, x.ip, x.port, x.password)

          val connStatus = eslConn.connectEsl()
          connStatus.isConnected match {
            case true =>
              log info "----> connection succeded "
              val resp = ApiReply(s"Connection Ok")
              wSLiveEventsActor ! ActorsJsonProtocol.caseClassToJsonMessage(resp)
              sender ! resp

              val newMap = connections updated(ip, eslConn)
              actorConnections = newMap
              context become idle(newMap)
            case false =>
              log info "Connection failed for ip " + x.ip
              val resp = ApiReply(connStatus.getMessage)
                context stop eslEventRouter
              wSLiveEventsActor ! ActorsJsonProtocol.caseClassToJsonMessage(resp)
              sender ! resp
          }

        case Some(actor) =>
          log info "we already have this ip connection " + x.ip
          sender ! ApiReply(s"we already have this ip connection ${x.ip}")
      }

    case x @ ShutdownEslConnection(ip) =>
      connections get x.ip match {
        case None =>
          sender ! ApiReply("this connection doesn't exist")

        case Some(eslConnection) =>
          // close connection
          log info "----> shutdown connection with "
          eslConnection.deinitConnection()
          sender ! ApiReply("connection terminated")
      }

    case Tick =>
      connections.map{
        case (a,y) => y.checkConnection() match {
          case x if x.isConnected => "all good"
          case x if !x.isConnected =>
            sender ! ApiReply(x.getMessage)
            log warning(s"---> EslConnectionDispatcherActor | the connection with ip $a is down!!")
        }
        case _ => log info "---> EslConnectionDispatcherActor | empty connections Map"
      }

    case Terminated =>
      log warning "------> Actor EslConnectionDispatcherActor is terminated"

  }

  override def postStop() = {
    log warning "------> Actor EslConnectionDispatcherActor is terminated postStop"
    actorConnections.map{
      case (x,y) => y.deinitConnection()
    }
  }

  context.system.scheduler.schedule(60000 milliseconds,
    60000 milliseconds,
    self,
    Tick)

  def receive: Receive =
    idle(scala.collection.Map.empty[String, EslConnection])
}
