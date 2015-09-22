package gr.gnostix.freeswitch.actors

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Props, OneForOneStrategy, Actor, ActorLogging}
import gr.gnostix.freeswitch.actors.ActorsProtocol._
import org.scalatra.atmosphere.AtmosphereClient

/**
 * Created by rebel on 23/8/15.
 */
class CentralMessageRouter extends Actor with ActorLogging {

  override val supervisorStrategy =
    OneForOneStrategy() {
      case _ => Restart
    }

  // first we should start the WS actor, otherwise we get exception!!
  val wsLiveEventsActor = context.actorOf(Props[WSLiveEventsActor], "wsLiveEventsActor")
  val completedCallsActor = context.actorOf(Props[CompletedCallsActor], "completedCallsActor")
  val heartBeatActor = context.actorOf(Props(new HeartBeatActor(wsLiveEventsActor)), "heartBeatActor")
  val eslConnectionDispatcherActor = context.actorOf(Props(new EslConnectionDispatcherActor(wsLiveEventsActor)), "eslConnectionDispatcherActor")
  val callRouterActor = context.actorOf(Props(new CallRouter(wsLiveEventsActor, completedCallsActor)), "callRouter")
  val basicStatsActor = context.actorOf(Props(new BasicStatsActor(callRouterActor, completedCallsActor, wsLiveEventsActor)), "basicStatsActor")

  // start the first connection
  // eslConnectionDispatcherActor ! EslConnectionData("localhost", 8021, "ClueCon")
//  eslConnectionDispatcherActor ! EslConnectionData("192.168.43.128", 8021, "ClueCon")

//  eslConnectionDispatcherActor ! EslConnectionData("fs-instance.com", 8021, "ClueCon")
//  eslConnectionDispatcherActor ! EslConnectionData("10.0.0.128", 8021, "ClueCon")

  def receive: Receive = {
   // case x @ Event(headers) =>
   //   eslEventRouter ! x

    case x @ EslConnectionData(ip, port, password) =>
      //eslConnectionDispatcherActor ! EslConnectionData("localhost", 8021, "ClueCon")
      eslConnectionDispatcherActor forward x

    case x @ DelEslConnection(ip) =>
       eslConnectionDispatcherActor forward x

    case x @ GetEslConnections =>
      eslConnectionDispatcherActor forward x

    case x @ GetCompletedCalls =>
      completedCallsActor forward x

    case x @ (GetLastHeartBeat | GetAllHeartBeat) =>
      heartBeatActor forward x

    case x @ (GetConcurrentCalls | GetTotalConcurrentCalls | GetFailedCalls | GetFailedCallsByDate |
              GetTotalFailedCalls  )=>
      // api calls asking for data
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


    case x@GetChannelInfo(callUuid, channeluuid) =>
      callRouterActor forward x

    case x @ AddAtmoClientUuid(uuid)  =>
      log info "central actor received  AddAtmoClientUuid(uuid) "
      wsLiveEventsActor ! x

    case x @ RemoveAtmoClientUuid(uuid) =>
      log info "central actor received  RemoveAtmoClientUuid(uuid)"
      wsLiveEventsActor ! x

    case x => log warning "I don't get this message!! " + x.toString
  }

}
