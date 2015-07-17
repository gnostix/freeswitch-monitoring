package gr.gnostix.freeswitch.actors

import akka.actor.{Props, ActorLogging, Actor}

/**
 * Created by rebel on 17/7/15.
 */

object CallActor {
  def props(uuid: String): Props = Props(new CallActor(uuid))
}

class CallActor(uuid: String) extends Actor with ActorLogging {
  val start = System.currentTimeMillis()

  def receive: Receive = {
    case CallRouter.GetCallInfo(_) =>
      val now = System.currentTimeMillis()
      val delta = (now - start) / 1000.0
      val response = s"$uuid is up for $delta secs"
      log info response
      sender() ! response
  }
}
