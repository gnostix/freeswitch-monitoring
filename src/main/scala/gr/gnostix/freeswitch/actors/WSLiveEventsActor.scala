package gr.gnostix.freeswitch.actors

import akka.actor.{Actor, ActorLogging}
import akka.pattern.ask
import akka.util.Timeout
import gr.gnostix.freeswitch.actors.ActorsProtocol._
import org.atmosphere.cpr.{AtmosphereResource, AtmosphereResourceImpl}
import org.json4s.NoTypeHints
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.write
import org.scalatra.atmosphere._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

/**
 * Created by rebel on 29/8/15.
 */

sealed trait Kokoko {
  def eventName: String
}

case class Lala(eventName: String, vava: String) extends Kokoko

class WSLiveEventsActor extends Actor with ActorLogging {
  implicit val timeout = Timeout(1 seconds)

  implicit val formats = Serialization.formats(NoTypeHints)

  var atmoClientsUuid: List[String] = List()
  val Tick = "tick"

  def receive: Receive = {

    case x@AddAtmoClientUuid(uuid) =>
      log info s"adding new client with uuid ${x.uuid}"
      atmoClientsUuid ::= x.uuid

    case x@RemoveAtmoClientUuid(uuid) =>
      atmoClientsUuid = atmoClientsUuid.filter(_ != uuid)
      log info s"removing client with uuid ${x.uuid} and client id list $atmoClientsUuid"

    case x: BasicStatsCalls =>
      atmoClientsUuid.size match {
        case 0 => log info "do nothing.. no connected clients.."
        case _ =>
          AtmosphereClient.lookup("/fs-moni/live/events").map { a =>
            a.broadcast(caseClassToJson(x))
          }

      }

    //case x: OutboundMessage =>
    case x: EventType =>
      log info "OutboundMessage coming .. " + x.eventName
      atmoClientsUuid.size match {
        case 0 => log info "do nothing.. no connected clients.."
        case _ =>
          //          log info s"the atmoClients list $atmoClientsUuid"
          AtmosphereClient.lookup("/fs-moni/live/events").foreach((broadcaster: ScalatraBroadcaster) => {
            val myResources = broadcaster.getAtmosphereResources.asScala filter { r => atmoClientsUuid.contains(r.uuid()) }
            myResources foreach ((resource: AtmosphereResource) => {
              val session = resource.getRequest.getSession(false)

              if (session == null) {
                //              log warning (s"-------> Encountered atmosphere resource ${resource.uuid()} associated with invalid session")
                resource.asInstanceOf[AtmosphereResourceImpl]._destroy()
              } else {
                //              log info s"Atmo: this client belongs to this channel and is logged in, id: ${resource.uuid()}"
                resource.getBroadcaster.broadcast(caseClassToJson(x))
              }
            })
          })

      }


    case Tick =>
      //to extend the session lifetime, use bellow code - https://github.com/scalatra/scalatra/issues/387
      AtmosphereClient.lookup("/fs-moni/live/events").foreach((broadcaster: ScalatraBroadcaster) => {
        val myResources = broadcaster.getAtmosphereResources.asScala filter { r => atmoClientsUuid.contains(r.uuid()) }
        myResources foreach ((resource: AtmosphereResource) => {
          val session = resource.getRequest.getSession(false)
          session.setMaxInactiveInterval(session.getMaxInactiveInterval + 900)
          log info (s"Extended life of session ${session.getId} for another 15 minutes " +
            s"maximum inactivity period in seconds of ${session.getMaxInactiveInterval}")
        })
      })
    case _ => log warning "WSLiveEventsActor | Unknown Message .."
  }

  def caseClassToJson(event: AnyRef) = {
    write(event)
  }

  def broadcastByUuid(uuid: String, events: List[AnyRef]) = {
    AtmosphereClient.lookup("/fs-moni/live/events").foreach((broadcaster: ScalatraBroadcaster) => {
      val myResources = broadcaster.getAtmosphereResources.asScala filter { r => uuid == r.uuid() }
      myResources foreach ((resource: AtmosphereResource) => {
        val session = resource.getRequest.getSession(false)

        if (session == null) {
          //              log warning (s"-------> Encountered atmosphere resource ${resource.uuid()} associated with invalid session")
          resource.asInstanceOf[AtmosphereResourceImpl]._destroy()
        } else {
          //              log info s"Atmo: this client belongs to this channel and is logged in, id: ${resource.uuid()}"
          events.map(event => resource.getBroadcaster.broadcast(caseClassToJson(event)))
        }
      })
    })
  }

  context.system.scheduler.schedule(3000 milliseconds,
    600000 milliseconds,
    self,
    Tick)

}
