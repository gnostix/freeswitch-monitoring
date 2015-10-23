/*
 * Copyright (c) 2015 Alexandros Pappas p_alx hotmail com
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package gr.gnostix.freeswitch.actors

import akka.actor._
import gr.gnostix.freeswitch.actors.ActorsProtocol._

/**
 * Created by rebel on 7/8/15.
 */


object ChannelActor {
  def props(channelStates: List[CallEventType]): Props = Props(new ChannelActor(channelStates))
}


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

    case x@NumberDialCodeCountry(toNumber, prefix, country) =>
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

          case ev => ev
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

    case x@GetConcurrentCallsChannel(uuid) =>
      //log info s"---------------> channel states ${channelStates}"
      //log info "channel actor got event GetConcurrentCallsChannel"
      (channelStates.head.asInstanceOf[CallNew].uuid == x.uuid) match {
        case true =>
          //log info "call router got event GetConcurrentCallsChannel TRUE"
          sender ! Some(channelStates.head)
        case false => sender ! None
      }


    case x@GetConcurrentCallsChannelByIpPrefix(ip, prefix) =>
      ip match {
        case None => prefix match {
          case None => sender ! Some(channelStates.head)
          case Some(pr) =>
            if (channelStates.head.toUser.startsWith(pr)) sender ! Some(channelStates.head) else sender ! None
        }

        case Some(ipAddr) => prefix match {
          case None => if (channelStates.head.freeSWITCHIPv4 == ipAddr) {
            sender ! Some(channelStates.head)
          } else {
            sender ! None
          }
          case Some(pr) =>
            if (channelStates.head.toUser.startsWith(pr) && channelStates.head.freeSWITCHIPv4 == ipAddr) {
              sender ! Some(channelStates.head)
            } else sender ! None
        }

      }


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