package gr.gnostix.freeswitch.actors

import akka.actor.{Actor, ActorLogging}

/**
 * Created by rebel on 17/8/15.
 */
class BasicStatsActor extends Actor with ActorLogging {

  def receive: Receive = {
    case FailedCall =>
    case CallNew =>
    case CallEnd =>
  }

}
