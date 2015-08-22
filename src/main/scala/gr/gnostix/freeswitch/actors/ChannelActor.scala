package gr.gnostix.freeswitch.actors

import akka.actor._

/**
 * Created by rebel on 7/8/15.
 */

/*
  object ChannelActor {
    def props(channelA: CallNew): Props = Props(new CallActor(channelA))
  }
*/

  class ChannelActor extends Actor with ActorLogging {
    var channelState = scala.collection.Map.empty[String, CallEventType]

    def receive: Receive = {

      case x @ CallNew(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, callUUID,
      callerChannelCreatedTime, callerChannelAnsweredTime, freeSWITCHHostname, freeSWITCHIPv4) =>
        //channelState = channelState updated(uuid, x)
        channelState += (uuid -> x)

        log info s" channel actor state Map $channelState"

      case x@CallEnd(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, callUUID,
      callerChannelCreatedTime, callerChannelAnsweredTime, callerChannelHangupTime, freeSWITCHHostname,
      freeSWITCHIPv4, hangupCause, billSec, rtpQualityPerc, otherLegUniqueId) =>
        //context.parent ! " tell that this actor channel is terminating so the parent can terminate to"
        //context.parent ! Terminated
        context stop self

      case CallRouter.GetChannelInfo(callUuid, channeluuid) =>
        val response = channelState
        log info s"channel response $response"
        sender ! response

      case x @ CallRouter.GetCallInfo(callUUID) =>
        log info "channel actor sending his state"
        sender ! channelState.head._2

      case _ => log info s"message  not understood on channelActor"
    }
  }