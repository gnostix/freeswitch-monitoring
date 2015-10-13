package gr.gnostix.freeswitch.actors

import akka.actor._
import gr.gnostix.freeswitch.actors.ActorsProtocol._

/**
 * Created by rebel on 7/8/15.
 */

/*
  object ChannelActor {
    def props(channelA: CallNew): Props = Props(new CallActor(channelA))
  }
*/

class ChannelActor(channelStates: List[CallEventType]) extends Actor with ActorLogging {
  //var channelStates: List[CallEventType] = List()

  def idle(channelStates: List[CallEventType]): Receive = {

    case x@CallNew(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, toUserIP, callUUID,
    callerChannelCreatedTime, callerChannelAnsweredTime, freeSWITCHHostname, freeSWITCHIPv4, callDirection,
    pdd, ringTimeSec, dialCode, country) =>
      //channelState = channelState updated(uuid, x)
      val newList = channelStates ::: List(x)
      context become idle(newList)

    //log info s" channel actor state Map on CallNew $channelState"

    case x@CallEnd(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, toUserIP, callUUID,
    callerChannelCreatedTime, callerChannelAnsweredTime, callerChannelHangupTime, freeSWITCHHostname,
    freeSWITCHIPv4, hangupCause, billSec, rtpQualityPerc, otherLegUniqueId, hangupDisposition, callDirection, mos,
    pdd, ringTimeSec, dialCode, country) =>
      //context stop self
      // send message to parrent tha the channel is terminated
      val newList = channelStates ::: List(x)
      context become idle(newList)

      context.parent ! CallTerminated(x)
    //log info s" channel actor state List on CallEnd $channelState"

    case x@NumberDialCodeCountry(prefix, country) =>
      val newList = channelStates.map {
        s => s match {
          case c: CallNew =>
            if (c.country == None) {
              CallNew(c.uuid, c.eventName, c.fromUser, c.toUser,
                c.readCodec, c.writeCodec, c.fromUserIP, c.toUserIP,
                c.callUUID, c.callerChannelCreatedTime, c.callerChannelAnsweredTime,
                c.freeSWITCHHostname, c.freeSWITCHIPv4, c.callDirection,
                c.pdd, c.ringingSec, x.prefix, x.country)
            } else c

          case c: CallEnd =>
            if (c.country == None) {
              CallEnd(c.uuid, c.eventName, c.fromUser, c.toUser, c.readCodec, c.writeCodec, c.fromUserIP, c.toUserIP,
                c.callUUID, c.callerChannelCreatedTime, c.callerChannelAnsweredTime, c.callerChannelHangupTime,
                c.freeSWITCHHostname, c.freeSWITCHIPv4, c.hangupCause, c.billSec, c.rtpQualityPerc, c.otherLegUniqueId,
                c.hangupDisposition, c.callDirection, c.mos, c.pdd, c.ringingSec, x.prefix, x.country)
            } else c

          case x => x
        }
      }
      context become idle(newList)


    case GetChannelInfo(callUuid, channeluuid) =>
      val response = channelStates
      //log info s"channel response $response"
      sender ! response

    case x@GetCallInfo(callUUID) =>
      //log info "channel actor sending his state"
      sender ! channelStates.map {
        case x: CallEnd => x
        case _ =>
      }

    case x@GetConcurrentCallsChannel =>
      log info s"---------------> channel states ${channelStates}"
      sender ! channelStates.head

/*
    case x@GetCompletedCallsChannel =>
      //log info s"channel actor channels $channelState and sending ${channelState.head}"
      sender ! channelStates.last
*/

    case _ => log info s"message  not understood on channelActor"
  }

  def receive: Receive =
    idle(channelStates)

}