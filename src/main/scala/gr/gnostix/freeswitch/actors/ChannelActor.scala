package gr.gnostix.freeswitch.actors

import akka.actor.{ActorLogging, Actor, Props}

/**
 * Created by rebel on 7/8/15.
 */

  object ChannelActor {
    def props(channelA: CallNew): Props = Props(new CallActor(channelA))
  }

  class ChannelActor(channel: CallNew) extends Actor with ActorLogging {
    var channelA = channel

    def receive: Receive = {
      case CallRouter.GetChannelInfo =>
        val response = channel
        log info response.toString
        sender() ! response

      case x@CallEnd(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, callUUID,
      callerChannelCreatedTime, callerChannelAnsweredTime, callerChannelHangupTime, freeSWITCHHostname,
      freeSWITCHIPv4, hangupCause) =>
        context.parent ! " tell that this actor channel is terminating so the parent can terminate to"
        context stop self
    }
  }