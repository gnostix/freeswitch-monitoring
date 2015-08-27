package gr.gnostix.freeswitch.actors

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Props, OneForOneStrategy, Actor, ActorLogging}
import gr.gnostix.freeswitch.actors.ActorsProtocol._

/**
 * Created by rebel on 23/8/15.
 */
class CentralMessageRouter extends Actor with ActorLogging {

  override val supervisorStrategy =
    OneForOneStrategy() {
      case _ => Restart
    }
  // create the HeartBeatActor in advance so we can keep the FS state
 // val eslEventRouter = context.actorOf(Props[EslEventRouter], "eslEventRouter")
  // start the BasicStatsActor actor
  val basicStatsActor = context.actorOf(Props[BasicStatsActor], "basicStatsActor")


  // start the CompletedCallsActor
  val completedCallsActor = context.actorOf(Props[CompletedCallsActor], "completedCallsActor")

  // start the generic ESL connection Actor
  // create the HeartBeatActor in advance so we can keep the FS state
  val heartBeatActor = context.actorOf(Props[HeartBeatActor], "heartBeatActor")
  val callRouterActor = context.actorOf(Props[CallRouter], "callRouter")
  val eslConnectionDispatcherActor = context.actorOf(Props[EslConnectionDispatcherActor], "eslConnectionDispatcherActor")

  // start the first connection
  eslConnectionDispatcherActor ! EslConnectionData("localhost", 8021, "ClueCon")
//  eslConnectionDispatcherActor ! EslConnectionData("192.168.43.128", 8021, "ClueCon")

  def receive: Receive = {
   // case x @ Event(headers) =>
   //   eslEventRouter ! x

    case x @ GetCompletedCalls =>
      completedCallsActor forward x

    case x @ (GetLastHeartBeat | GetAllHeartBeat) =>
      heartBeatActor forward x

    case x @ (GetCalls | GetConcurrentCalls | GetFailedCalls | GetFailedCallsByDate |
              GetTotalFailedCalls  )=>
      // api calls asking for data
      callRouterActor forward x

    case x@(GetConcurrentCallsTimeSeries | GetFailedCallsTimeSeries) =>
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



  }

}