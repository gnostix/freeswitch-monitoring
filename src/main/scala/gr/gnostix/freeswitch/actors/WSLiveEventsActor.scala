package gr.gnostix.freeswitch.actors

import akka.actor.{Actor, ActorLogging}
import gr.gnostix.freeswitch.actors.ActorsProtocol.{AddAtmoClientUuid, RemoveAtmoClientUuid}
import org.atmosphere.cpr.{AtmosphereResourceImpl, BroadcasterFactory, Broadcaster, AtmosphereResource}
import org.json4s.NoTypeHints
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.write
import org.scalatra.atmosphere._
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Created by rebel on 29/8/15.
 */

sealed trait Kokoko {
  def eventName: String
}

case class Lala(eventName: String, vava: String) extends Kokoko

class WSLiveEventsActor extends Actor with ActorLogging {

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
          log info s"the atmoClients list $atmoClientsUuid"

          // this has to be reworked and refined!!
          // remove invalidated AtmosphereResources / http sessions
          AtmosphereClient.lookup("/fs-moni/live/events").foreach(_.getAtmosphereResources.asScala.toSet foreach (
            (resource: AtmosphereResource) =>
              if (resource.getRequest.getSession(false) == null) {
                log warning (s"-------> Encountered atmosphere resource ${resource.uuid()} associated with invalid session")
                resource.asInstanceOf[AtmosphereResourceImpl]._destroy()

              } else {
                //check if this resource is in our special client list (logged in to the right account!!)
                atmoClientsUuid.contains(resource.uuid()) match {
                  case true =>
                    log info s"Atmo: this client belongs to this channel and is logged in, id: ${resource.uuid()}"
                    resource.getBroadcaster.broadcast(caseClassToJson(x))

                  case false =>
                    log info s"this client is doesn't  belong to this channel, id: ${resource.uuid()}"
                    //resource.asInstanceOf[AtmosphereResourceImpl]._destroy()
                }
                //log info s"the atmo resources are: $resource"

              }
            ))

      }

    case Tick =>
      //to extend the session lifetime, use bellow code - https://github.com/scalatra/scalatra/issues/387
      AtmosphereClient.lookup("/fs-moni/live/events").foreach( (broadcaster:ScalatraBroadcaster) => {
        val myResources = broadcaster.getAtmosphereResources.asScala filter {r => atmoClientsUuid.contains(r.uuid())}
        myResources foreach( (resource:AtmosphereResource) => {
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


  context.system.scheduler.schedule(3000 milliseconds,
    600000 milliseconds,
    self,
    Tick)

}
