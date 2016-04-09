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

import akka.actor.SupervisorStrategy.Restart
import akka.actor._
import gr.gnostix.freeswitch.actors.ActorsProtocol._
import gr.gnostix.freeswitch.actors.ServletProtocol.{ApiReplyError, ApiReplyData}
import gr.gnostix.freeswitch.utilities.FileUtilities
import org.scalatra.atmosphere.AtmosphereClient

import scala.collection.SortedMap

/**
 * Created by rebel on 23/8/15.
 */

object CentralMessageRouter {
  def props(dialCodes: Map[String, SortedMap[String, String]]): Props =
    Props(new CentralMessageRouter(dialCodes: Map[String, SortedMap[String, String]]))
}

class CentralMessageRouter(dialCodes: Map[String, SortedMap[String, String]]) extends Actor with ActorLogging {

  override val supervisorStrategy =
    OneForOneStrategy() {
      case _ => Restart
    }

  // first we should start the WS actor, otherwise we get exception!!
  val wsLiveEventsActor = context.actorOf(Props[WSLiveEventsActor], "wsLiveEventsActor")
  val completedCallsActor = context.actorOf(Props[CompletedCallsActor], "completedCallsActor")
  val heartBeatActor = context.actorOf(Props(classOf[HeartBeatActor],wsLiveEventsActor), "heartBeatActor")
  val eslConnectionDispatcherActor = context.actorOf(Props(classOf[EslConnectionDispatcherActor], wsLiveEventsActor), "eslConnectionDispatcherActor")
  val callRouterActor = context.actorOf(Props(classOf[CallRouter], wsLiveEventsActor, completedCallsActor), "callRouter")
  val basicStatsActor = context.actorOf(Props(classOf[BasicStatsActor], callRouterActor, completedCallsActor, wsLiveEventsActor), "basicStatsActor")

  val dialCodesFile = FileUtilities.processResourcesCsvFile()
  val dialCodesActor = dialCodesFile match {
      case x: ApiReplyData =>
        context.actorOf(Props(new DialCodesActor(x.payload.asInstanceOf[Map[String, SortedMap[String, String]]])), "dialCodesActor")
      case x: ApiReplyError =>
        wsLiveEventsActor ! x.message
        context.actorOf(Props(new DialCodesActor(Map("default" -> scala.collection.SortedMap.empty[String, String]))), "dialCodesActor")
      case _ =>
        log warning "we don't understand the reply back from dial codes actor when trying to upload a file"
        context.actorOf(Props(new DialCodesActor(Map("default" -> scala.collection.SortedMap.empty[String, String]))), "dialCodesActor")
    }


  // start the first connection
  //eslConnectionDispatcherActor ! EslConnectionData("localhost", 8022, "ClueCon")
   eslConnectionDispatcherActor ! EslConnectionData("192.168.1.128", 8021, "ClueCon")

  eslConnectionDispatcherActor ! EslConnectionData("fs-instance.com", 8021, "ClueCon")
  //  eslConnectionDispatcherActor ! EslConnectionData("10.0.0.128", 8021, "ClueCon")

  def receive: Receive = {
    // case x @ Event(headers) =>
    //   eslEventRouter ! x

    case x@GetConcurrentCallsChannel =>
      callRouterActor forward x

    case x@GetFailedCallsChannel =>
      callRouterActor forward x

    case x@GetCompletedCallsChannel =>
      completedCallsActor forward x


    case x@GetFailedCallsAnalysis(fromNumberOfDigits, toNumberOfDigits) =>
      callRouterActor forward x

    case x@EslConnectionData(ip, port, password) =>
      //eslConnectionDispatcherActor ! EslConnectionData("localhost", 8021, "ClueCon")
      eslConnectionDispatcherActor forward x

    case x@DelEslConnection(ip) =>
      eslConnectionDispatcherActor forward x

    case x@GetEslConnections =>
      eslConnectionDispatcherActor forward x

    case x@GetCompletedCalls =>
      completedCallsActor forward x

    case x@GetBillSecAndRTPByCountry =>
      completedCallsActor forward x

    case x@(GetLastHeartBeat | GetAllHeartBeat) =>
      heartBeatActor forward x

    case x@(GetConcurrentCalls | GetTotalConcurrentCalls | GetFailedCalls | GetFailedCallsByDate |
            GetTotalFailedCalls) =>
      // api calls asking for data
      callRouterActor forward x

    case x @ GetConcurrentCallsChannelByIpPrefix(ip, prefix) =>
      callRouterActor forward x

    case x@(GetBasicStatsTimeSeries) =>
      // api calls asking for data
      basicStatsActor forward x

    /*    case x@GetConcurrentCalls =>
          // log info "call router GetConcurrentCalls received .."
          callRouterActor forward x

        case x@GetFailedCalls =>
          log info "--------> ask for failed calls"
          callRouterActor forward x

        case x@GetFailedCallsByDate =>
          callRouterActor forward x

        case x@GetTotalFailedCalls =>
          callRouterActor forward x
    */
    case x@GetCallInfo(callUuid) =>
      callRouterActor forward x

    case x@InitializeDashboardBasicStats =>
      basicStatsActor forward x

    case x@InitializeDashboardHeartBeat =>
      heartBeatActor forward x

    case x@GetChannelInfo(callUuid, channeluuid) =>
      callRouterActor forward x

    case x@AddAtmoClientUuid(uuid) =>
      //log info "central actor received  AddAtmoClientUuid(uuid) "
      wsLiveEventsActor ! x

    case x@RemoveAtmoClientUuid(uuid) =>
      //log info "central actor received  RemoveAtmoClientUuid(uuid)"
      wsLiveEventsActor ! x

    case x @ GetDialCodeList(fileName) =>
      dialCodesActor forward x

    case x @ DelDialCodeList(fileName) =>
      dialCodesActor forward x

    case x @ AddDialCodeList(filename, dialCodesS) =>
      dialCodesActor forward x

    case x @ GetAllDialCodeList =>
      dialCodesActor forward x

    case x => log warning "I don't get this message!! " + x.toString
  }

}
