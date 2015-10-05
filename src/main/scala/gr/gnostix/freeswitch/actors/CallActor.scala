package gr.gnostix.freeswitch.actors

import java.sql.Timestamp

import akka.actor.SupervisorStrategy.Restart
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import gr.gnostix.freeswitch.actors.ActorsProtocol._
import org.scalatra.atmosphere.AtmosphereClient
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import scala.concurrent.Future

/**
 * Created by rebel on 17/7/15.
 */


case class CompletedCallStats(acd: Int, rtpQuality: Double, callerChannelHangupTime: Timestamp)

class CallActor extends Actor with ActorLogging {


  override val supervisorStrategy =
    OneForOneStrategy() {
      case _ => Restart
    }

  implicit val timeout = Timeout(1 seconds) // needed for `?` below

  var terminatedChannels = 0
  var callUuid: Option[String] = None
  var endCallChannel: Option[CallEnd] = None

  def idle(activeChannels: scala.collection.Map[String, ActorRef]): Receive = {

    case x@CallNew(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, toUserIP, callUUID,
    callerChannelCreatedTime, callerChannelAnsweredTime, freeSWITCHHostname, freeSWITCHIPv4) =>
      log info s"======== in call actor $x"
      (activeChannels get uuid) match {
        case None =>
          val actor = context.actorOf(Props[ChannelActor], uuid)
          actor ! x
          context watch actor

//          log warning s"We create the actor Channel $uuid"
          val newMap = activeChannels updated(uuid, actor)
          context become idle(newMap)
        case Some(actor) =>
          log warning s"We have this Channel $uuid"
      }

    case x@CallEnd(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, toUserIP, callUUID,
    callerChannelCreatedTime, callerChannelAnsweredTime, callerChannelHangupTime, freeSWITCHHostname,
    freeSWITCHIPv4, hangupCause, billSec, rtpQualityPerc, otherLegUniqueId) =>
      (activeChannels get uuid) match {
        case None =>
          log warning s"Channel $uuid not found"
        case Some(actor) =>

          endCallChannel match {
            case Some(x) => //AtmosphereClient.broadcast("/fs-moni/live/events", ActorsJsonProtocol.endCallToJson(x))

            case None => endCallChannel = Some(x)
          }

          actor ! x
      }

    case x@ActorsProtocol.GetChannelInfo(callUuid, channeluuid) =>
      log info s"-----> Channel $channeluuid  in callActor sender " + sender.toString
      //log info s"-----> Channels " + activeChannels.toString()

      (activeChannels get channeluuid) match {
        case None =>
          log warning s"Channel $channeluuid not found  in callActor"
        case Some(actor) =>
          log warning s"Channel $channeluuid found in callActor"
          actor forward x
      }

    case x@ActorsProtocol.GetCallInfo(callUUID) =>
      (callUuid.getOrElse("") == callUUID) match {
        case false => sender ! "Unknown call uuid"
        case true =>

          val all: Future[List[Any]] = activeChannels.size match {
            case 2 => for {
              chA <- (activeChannels.head._2 ask x)
              chB <- (activeChannels.tail.head._2 ask x)
            } yield (List(chA, chB))

            case 1 => for {
              chA <- (activeChannels.head._2 ask x)
            } yield (List(chA))

          }

          sender ! all
      }

    case x @ (GetConcurrentCallsChannel | GetCompletedCallsChannel) =>
      activeChannels.head._2 forward x


    case x@GetACDAndRTP =>
      //log info s"--------> CallActor GetACDLastFor60Seconds: $x"
      endCallChannel.headOption match {
        case Some(a) =>
          //log info s"--------> CallActor on CompletedCalls: $a"
          sender ! CompletedCallStats(a.billSec, a.rtpQualityPerc, a.callerChannelHangupTime)
        case None => log warning "-----> ignore GetACDLastFor60Seconds.. channel empty!!"
      }

    case CallTerminated(callEnd) =>
      terminatedChannels += 1
      //log info s"call actor channel is terminated " + terminatedChannels
      val updatedActiveChannels = activeChannels.filter(_._2 != sender())

      if ((terminatedChannels >= 2) || (updatedActiveChannels.size == 0)) {
        log info s"this call is terminated "
        context.parent ! CallTerminated(callEnd)

        //AtmosphereClient.broadcast("/fs-moni/live/events", ActorsJsonProtocol.endCallToJson(endCallChannel.get))
      }


    /*    case Terminated(actor: ActorRef) =>
          terminatedChannels += 1
          log info s"call actor TERMINATED " + terminatedChannels

          val updatedActiveChannels = activeChannels.filter(_._2 != sender())

          if ((terminatedChannels >= 2) || (updatedActiveChannels.size == 0) ) {
            log info s"this call is terminated "
            AtmosphereClient.broadcast("/fs-moni/live/events", ActorsJsonProtocol.endCallToJson(endCallChannel.get))

            context stop self
          }
          context become idle(updatedActiveChannels)*/

    case x@_ =>
      log.info("---- call actor - I don't know this channel uuid " + x)
  }

  def receive: Receive =
    idle(scala.collection.Map.empty[String, ActorRef])
}

