package gr.gnostix.freeswitch.actors

import akka.actor.{ActorRef, Props, ActorLogging, Actor}
import gr.gnostix.freeswitch.actors.CallRouter.Event
import org.json4s.JsonAST.{JString, JObject}
import org.scalatra.atmosphere.{AtmosphereClient, JsonMessage, TextMessage}

/**
 * Created by rebel on 17/7/15.
 */

object CallActor {
  def props(channelA: CallNew): Props = Props(new CallActor(channelA))
}

class CallActor(channelA: CallNew) extends Actor with ActorLogging {

  def idle(activeChannels: scala.collection.Map[String, ActorRef]): Receive = {

    case x @ CallNew(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, callUUID,
    callerChannelCreatedTime, callerChannelAnsweredTime, freeSWITCHHostname, freeSWITCHIPv4) =>
      log info s"======== $x"
      (activeChannels get uuid) match {
        case None =>
          val actor = context actorOf CallActor.props(x)
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

    case x @ CallRouter.GetChannelInfo(uuid) =>
      (activeChannels get uuid) match {
        case None =>
          log warning s"Channel $uuid not found"
        case Some(actor) =>
          actor forward x
      }

    case "channel_terminate with uuid" =>
    // check if active channels are 0 then stop self

    case _ =>
      log.info("---- I don't know this channel uuid")
  }

  def receive: Receive =
    idle(scala.collection.Map.empty[String, ActorRef])
}

