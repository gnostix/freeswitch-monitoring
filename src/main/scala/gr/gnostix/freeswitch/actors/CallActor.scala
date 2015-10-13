package gr.gnostix.freeswitch.actors

import java.sql.Timestamp

import akka.actor.SupervisorStrategy.Restart
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import gr.gnostix.freeswitch.actors.ActorsProtocol._
import gr.gnostix.freeswitch.model.{CompletedCallStatsByCountry, CompletedCallStats}
import org.scalatra.atmosphere.AtmosphereClient
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import scala.concurrent.Future

/**
 * Created by rebel on 17/7/15.
 */


class CallActor extends Actor with ActorLogging {


  override val supervisorStrategy =
    OneForOneStrategy() {
      case _ => Restart
    }

  implicit val timeout = Timeout(1 seconds) // needed for `?` below

  var terminatedChannels = 0
  var callUuid: Option[String] = None
  var endCallChannel: Option[CallEnd] = None
  var uuidChannelA: String = ""
  var countryT: Option[String] = None
  var dialCodeT: Option[String] = None
  var checkedPrefix = false

  val dialCodesActor = context.actorSelection("/user/centralMessageRouter/dialCodesActor")

  def idle(activeChannels: scala.collection.Map[String, ActorRef]): Receive = {

    case x@CallNew(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, toUserIP, callUUID,
    callerChannelCreatedTime, callerChannelAnsweredTime, freeSWITCHHostname, freeSWITCHIPv4, callDirection,
    pdd, ringTimeSec, None, None) =>
      log info s"======== in call actor $x"
      (activeChannels get uuid) match {
        case None =>

          // set the leg A uuid for later reference
          if (x.callDirection.equalsIgnoreCase("outbound")) {
            uuidChannelA = x.uuid
          }

          // set if available the country prefix
          val newCall = (countryT != None) match {
            case true =>
              CallNew(x.uuid, x.eventName, x.fromUser, x.toUser, x.readCodec, x.writeCodec, x.fromUserIP, x.toUserIP,
                x.callUUID, x.callerChannelCreatedTime, x.callerChannelAnsweredTime, x.freeSWITCHHostname, x.freeSWITCHIPv4,
                x.callDirection, x.pdd, x.ringingSec, dialCodeT, countryT)

            case false =>
              // we check for the prefix only once
              if (checkedPrefix == false) {
                dialCodesActor ! GetNumberDialCode(toUser)
              }

              x
          }

          val actor = context.actorOf(Props(new ChannelActor(List(newCall))), uuid)

          context watch actor
          // log warning s"We create the actor Channel $uuid"
          val newMap = activeChannels updated(uuid, actor)
          context become idle(newMap)


        case Some(actor) =>
          log warning s"We have this Channel $uuid"
      }

    case x@CallEnd(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, toUserIP, callUUID,
    callerChannelCreatedTime, callerChannelAnsweredTime, callerChannelHangupTime, freeSWITCHHostname,
    freeSWITCHIPv4, hangupCause, billSec, rtpQualityPerc, otherLegUniqueId, hangupDisposition, callDirection, mos,
    pdd, ringTimeSec, None, None) =>
      (activeChannels get x.uuid) match {
        case None =>
          log warning s"Channel $uuid not found"
        case Some(actor) =>

          if (countryT != None) {
            val endCall = CallEnd(x.uuid, x.eventName, x.fromUser, x.toUser, x.readCodec, x.writeCodec, x.fromUserIP,
              x.toUserIP, x.callUUID, x.callerChannelCreatedTime, x.callerChannelAnsweredTime, x.callerChannelHangupTime,
              x.freeSWITCHHostname, x.freeSWITCHIPv4, x.hangupCause, x.billSec, x.rtpQualityPerc, x.otherLegUniqueId,
              x.hangupDisposition, x.callDirection, x.mos, x.pdd, x.ringingSec, dialCodeT, countryT)

            // set as the end channel, the Channel A
            if (x.uuid.equalsIgnoreCase(uuidChannelA)) {
              endCallChannel = Some(endCall)
            }

            actor ! endCall
          } else {
            // set as the end channel, the Channel A
            if (x.uuid.equalsIgnoreCase(uuidChannelA)) {
              endCallChannel = Some(x)
            }

            actor ! x
            dialCodesActor ! GetNumberDialCode(toUser)
          }

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

    case x @ GetConcurrentCallsChannel =>
      activeChannels.head._2 forward x

    case x @ GetCompletedCallsChannel =>
      endCallChannel match {
        case Some(a) =>
          //log info s"--------> CallActor on CompletedCalls: $a"
          sender ! endCallChannel
        case None => log warning "-----> ignore GetCompletedCallsChannel.. channel empty!!"
      }


    case x@GetACDAndRTP =>
      //log info s"--------> CallActor GetACDLastFor60Seconds: $x"
      endCallChannel match {
        case Some(a) =>
          //log info s"--------> CallActor on CompletedCalls: $a"
          sender ! CompletedCallStats(a.billSec, a.rtpQualityPerc, a.callerChannelHangupTime)
        case None => log warning "-----> ignore GetACDLastFor60Seconds.. channel empty!!"
      }

    case x@GetACDAndRTPByCountry =>
      //log info s"--------> CallActor GetACDLastFor60Seconds: $x"
      endCallChannel match {
        case Some(a) =>
          //log info s"--------> CallActor on CompletedCalls: $a"
          a.dialCode match {
            case Some(code) => sender ! Some(CompletedCallStatsByCountry(a.dialCode, a.country, a.billSec, a.rtpQualityPerc, a.callerChannelHangupTime))
            case None => sender ! None
          }
        case None => log warning "-----> ignore GetACDAndRTPByCountry.. channel empty!!"
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


    // IF THE CALL IS OLDER THAN 6 HOURS THEN WE SHULD SEND IT IN THE COMPLETED CALLS
    // THIS IS FOR THE CASES OF FREESWITCH RESTART WHEN WE ARE LEFT  WITH UNCOMPLETED CALLS..

    case x@NumberDialCodeCountry(prefix, countr) =>
      x.prefix match {
        case Some(dt) =>
          countryT = x.country
          dialCodeT = x.prefix
          checkedPrefix = true

          endCallChannel match {
            case Some(c) => endCallChannel = Some(CallEnd(c.uuid, c.eventName, c.fromUser, c.toUser, c.readCodec, c.writeCodec,
              c.fromUserIP, c.toUserIP, c.callUUID, c.callerChannelCreatedTime, c.callerChannelAnsweredTime, c.callerChannelHangupTime,
              c.freeSWITCHHostname, c.freeSWITCHIPv4, c.hangupCause, c.billSec, c.rtpQualityPerc, c.otherLegUniqueId, c.hangupDisposition,
              c.callDirection, c.mos, c.pdd, c.ringingSec, x.prefix, x.country))
            case None =>
          }
          activeChannels.map {
            ch => ch._2 ! x
          }
        case None =>
          // if we can't find the country then we flag this as unknown
          checkedPrefix = true
          countryT = Some("unknown")
          dialCodeT = Some("unknown")

          endCallChannel match {
            case Some(c) => endCallChannel = Some(CallEnd(c.uuid, c.eventName, c.fromUser, c.toUser, c.readCodec, c.writeCodec,
              c.fromUserIP, c.toUserIP, c.callUUID, c.callerChannelCreatedTime, c.callerChannelAnsweredTime, c.callerChannelHangupTime,
              c.freeSWITCHHostname, c.freeSWITCHIPv4, c.hangupCause, c.billSec, c.rtpQualityPerc, c.otherLegUniqueId, c.hangupDisposition,
              c.callDirection, c.mos, c.pdd, c.ringingSec, dialCodeT, countryT))
            case None =>
          }
      }


    case x@_ =>
      log.info("---- call actor - I don't know this channel uuid " + x)
  }

  def receive: Receive =
    idle(scala.collection.Map.empty[String, ActorRef])

}

