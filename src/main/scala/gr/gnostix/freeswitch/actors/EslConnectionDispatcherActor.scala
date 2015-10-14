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

package gr.gnostix.freeswitch.actors

import akka.actor._
import gr.gnostix.freeswitch.actors.ServletProtocol.ApiReply
import gr.gnostix.freeswitch.{EslConnection}
import gr.gnostix.freeswitch.actors.ActorsProtocol.{GetEslConnections, ShutdownEslConnection, EslConnectionData, DelEslConnection}
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

    case x @ GetEslConnections =>
      sender ! connections.map{
        case (ip,conn) => EslConnectionData(conn.getIP, conn.getPort,"the-secret")
      }.toList

    case x @ DelEslConnection(ip) =>
      connections get x.ip  match {
        case Some(eslConnection) =>
          // close connection
          log info s"----> shutdown connection with ip: ${x.ip} and connections: $connections"
          eslConnection.deinitConnection()
          val newMap = connections.filter(c => c._1 != x.ip)
          // stop the esl event actor for this connection
          context stop eslConnection.getActor()
          log info s"----> shutdown connection with connections: $newMap"
          actorConnections = newMap
          context become idle(newMap)
          sender ! ApiReply(200,"connection terminated")


          case None => sender ! ApiReply(400, "this ip doesn't exists")
      }
      

    case x @ EslConnectionData(ip, port, password) =>
      log info s"esl connections: $connections"
      connections get x.ip match {
        case None =>
          val eslEventRouter = context.actorOf(Props[EslEventRouter], x.ip)
          val eslConn = new EslConnection(eslEventRouter, x.ip, x.port, x.password)

          val connStatus = eslConn.connectEsl()
          connStatus.isConnected match {
            case true =>
              log info "----> connection succeded "
              val resp = ApiReply(200, "Connection is up")
              //wSLiveEventsActor ! resp
              //wSLiveEventsActor ! ActorsJsonProtocol.caseClassToJsonMessage(resp)
              sender ! resp

              val newMap = connections updated(ip, eslConn)
              actorConnections = newMap
              context become idle(newMap)
            case false =>
              log info "Connection failed for ip " + x.ip
              val resp = ApiReply(400, connStatus.getMessage)
                context stop eslEventRouter
              wSLiveEventsActor ! resp
              //wSLiveEventsActor ! ActorsJsonProtocol.caseClassToJsonMessage(resp)
              sender ! resp
          }

        case Some(eslConnection) =>
          log error s"we already have this ip connection ${x.ip} and connections: ${connections}"
          sender ! ApiReply(400, s"we already have this ip connection ${x.ip}")
      }


    case Tick =>
      connections.map{
        case (a,y) => y.checkConnection() match {
          case x if x.isConnected => "all good"
          case x if !x.isConnected =>
            sender ! ApiReply(400, x.getMessage)
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
