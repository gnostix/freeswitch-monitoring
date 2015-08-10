package gr.gnostix.freeswitch.actors

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

/**
 * Created by rebel on 17/7/15.
 */

/*object CallActor {
  def props(channelA: CallNew): Props = Props(new CallActor(channelA))
}*/

class CallActor extends Actor with ActorLogging {

/*
  override val supervisorStrategy =
    OneForOneStrategy() {
      case _ => Restart
    }
*/
implicit val timeout = Timeout(1 seconds) // needed for `?` below

  var terminatedChannels = 0
  var callUuid = ""

  def idle(activeChannels: scala.collection.Map[String, ActorRef]): Receive = {

    case x @ CallNew(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, callUUID,
    callerChannelCreatedTime, callerChannelAnsweredTime, freeSWITCHHostname, freeSWITCHIPv4) =>
      log info s"======== in call actor $x"
      (activeChannels get uuid) match {
        case None =>
          callUuid = callUUID
          //val actor = context actorOf ChannelActor.props(x)
          val actor = context.actorOf(Props[ChannelActor], uuid)
          actor ! x
          context watch actor

          log warning s"We create the actor Channel $uuid"
          val newMap = activeChannels updated(uuid, actor)
          context become idle(newMap)
        case Some(actor) =>
          log warning s"We have this Channel $uuid"
      }

    case x @ CallEnd(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, callUUID,
    callerChannelCreatedTime, callerChannelAnsweredTime, callerChannelHangupTime, freeSWITCHHostname,
    freeSWITCHIPv4, hangupCause) =>
      (activeChannels get uuid) match {
        case None =>
          log warning s"Channel $uuid not found"
        case Some(actor) =>
          actor ! x
      }

    case x @ CallRouter.GetChannelInfo(callUuid, channeluuid) =>
      log info s"-----> Channel $channeluuid  in callActor sender " + sender.toString
      log info s"-----> Channels " + activeChannels.toString()

      (activeChannels get channeluuid) match {
        case None =>
          log warning s"Channel $channeluuid not found  in callActor"
        case Some(actor) =>
          log warning s"Channel $channeluuid found in callActor"
          actor forward x
      }

    case x @ CallRouter.GetCallInfo(callUUID) =>
      (callUuid == callUUID) match {
        case false => sender ! "Unknown call uuid"
        case true =>

          val all: Future[List[Any]] = for {
            chA <- (activeChannels.head._2 ask x)
            chB <- (activeChannels.tail.head._2 ask x)
          } yield (List(chA, chB))
          sender ! all
      }


    case Terminated(actor: ActorRef) =>
      terminatedChannels += 1
      log info s"call actor TERMINATED " + terminatedChannels

      if (terminatedChannels >= 2) {
        log info s"this call is terminated "
        context stop self
      }

    case x @ _ =>
      log.info("---- call actor - I don't know this channel uuid " + x)
  }

  def receive: Receive =
    idle(scala.collection.Map.empty[String, ActorRef])
}

