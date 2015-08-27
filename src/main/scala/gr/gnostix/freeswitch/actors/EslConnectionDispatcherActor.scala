package gr.gnostix.freeswitch.actors

import akka.actor.{Props, Actor, ActorLogging}
import gr.gnostix.freeswitch.{EslConnection}
import gr.gnostix.freeswitch.actors.ActorsProtocol.{ShutdownEslConnection, EslConnectionData}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by rebel on 27/8/15.
 */
class EslConnectionDispatcherActor extends Actor with ActorLogging {

  val Tick = "tick"

  def idle(connections: scala.collection.Map[String, EslConnection]): Receive = {

    case x @ EslConnectionData(ip, port, password) =>
      connections get x.ip match {
        case None =>
          val eslEventRouter = context.actorOf(Props[EslEventRouter], "eslEventRouter")
          val eslConn = new EslConnection(eslEventRouter, x.ip, x.port, x.password)

          eslConn.connectEsl() match {
            case true =>
              log info "----> connection succeded "
              val newMap = connections updated(ip, eslConn)
              context become idle(newMap)
            case false =>
              log info "Connection failed for ip " + x.ip
              sender ! "ConnFailed"
          }

        case Some(actor) =>
          sender ! "----> we already have this ip connection"
      }

    case x @ ShutdownEslConnection(ip) =>
      connections get x.ip match {
        case None =>
          sender ! "----> this connection doesn't exist"

        case Some(eslConnection) =>
          // close connection
          log info "----> shutdown connection with "
          eslConnection.deinitConnection()
      }

    case Tick =>
      connections.map{
        case (x,y) => y.checkConnection() match {
          case true => "all good"
          case false => log warning(s"---> EslConnectionDispatcherActor | the connection with ip $x is down!!")
        }
        case _ => log info "---> EslConnectionDispatcherActor | empty connections Map"
      }

  }
  context.system.scheduler.schedule(60000 milliseconds,
    60000 milliseconds,
    self,
    Tick)

  def receive: Receive =
    idle(scala.collection.Map.empty[String, EslConnection])
}
