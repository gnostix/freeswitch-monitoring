package gr.gnostix.freeswitch.actors

import akka.actor.{Props, ActorLogging, Actor}

/**
 * Created by rebel on 17/7/15.
 */

object CallActor {
  def props(channelA: CallNew): Props = Props(new CallActor(channelA))
}

class CallActor(channelA: CallNew) extends Actor with ActorLogging {
  val chA = channelA
  var chB: Map[String,AnyRef] = Map()

  def receive: Receive = {
    case CallRouter.GetCallInfo(_) =>
      val response = s"${channelA.callUUID} details ${channelA.toString} "
      log info response
      sender() ! response
    case x @ CallEnd => chB += "uuid_alx" -> x
  }

}
