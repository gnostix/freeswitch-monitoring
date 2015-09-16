package gr.gnostix.freeswitch.actors

import akka.actor.{Actor, ActorLogging}
import gr.gnostix.freeswitch.actors.ActorsProtocol.{AddAtmoClientUuid, RemoveAtmoClientUuid}
import org.json4s.NoTypeHints
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.write
import org.scalatra.atmosphere._

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
          AtmosphereClient.lookup("/fs-moni/live/koko").map{a =>
            a.broadcast(heartbeatToJson(x))}

      }

    //case x: OutboundMessage =>
    case x: EventType =>
      log info "OutboundMessage coming .."
      //AtmosphereClient.broadcast("/fs-moni/live/koko", x)
      atmoClientsUuid.size match {
        case 0 => log info "do nothing.. no connected clients.."
        case _ =>
          log info s"the atmoClients list $atmoClientsUuid"
 /*         atmoClientsUuid.map(uuid => AtmosphereClient.broadcast("/fs-moni/live/koko", x, new ClientFilter(uuid) {
            override def apply(v1: AtmosphereResource): Boolean = true
          }))*/
          AtmosphereClient.lookup("/fs-moni/live/koko").map{a =>
            a.broadcast(heartbeatToJson(x))}

      }

    case Tick =>
      //AtmosphereClient.broadcast("/fs-moni/live/koko", heartbeatToJson(Lala("lololololo")))
      AtmosphereClient.lookup("/fs-moni/live/koko").map{x =>
        val it = x.getAtmosphereResources.iterator()
        while (it.hasNext) {
          val a = it.next()
          log info "next uuid is : " + a.uuid()
        }
        log info "-------- end of next uuid ----------"
        x.broadcast(heartbeatToJson(Lala("koko","lololololo")))}
      //log info "WS actore Tick " + heartbeatToJson(Lala("lololololo")).toString
     /* log info "----- " +AtmosphereResourceFactory.getDefault.find(atmoClientsUuid.head)
      .getBroadcaster.broadcast("{\"koko\" : \"kokokokokokok\"} ")*/
/*      atmoClientsUuid.map(uuid => AtmosphereClient.broadcast("/fs-moni/live/koko", heartbeatToJson(Lala("lololololo")),
        new ClientFilter(uuid) {
        override def apply(v1: AtmosphereResource): Boolean = true
      }))*/
      //AtmosphereClient.broadcast("/fs-moni/live/koko", heartbeatToJson(Lala("lololololo")))

    case _ => log warning "WSLiveEventsActor | Unknown Message .."
  }

  def heartbeatToJson(heartBeat: AnyRef) = {
    log info "-------Lala to json : " + write(heartBeat)
    write(heartBeat)
  }

  context.system.scheduler.schedule(3000 milliseconds,
    50000 milliseconds,
    self,
    Tick)

}
