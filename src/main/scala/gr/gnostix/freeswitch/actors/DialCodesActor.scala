package gr.gnostix.freeswitch.actors

import akka.actor.{Actor, ActorLogging}
import gr.gnostix.freeswitch.actors.ActorsProtocol._
import gr.gnostix.freeswitch.actors.ServletProtocol.{ApiReplyError, ApiReplyData, ApiReply}

import scala.collection.SortedMap

/**
 * Created by rebel on 10/10/15.
 */
class DialCodesActor(dialCodes: Map[String, SortedMap[String, String]]) extends Actor with ActorLogging {

  def idle(dialCodes: Map[String, SortedMap[String, String]]): Receive = {

    case x@AddDialCodeList(filename, dialCodesS) =>
      val newDialCodes = dialCodes + (x.fileName -> x.dialCodes)
      //sender ! ApiReply(200, "DialCodes added successfully")
      context become idle(newDialCodes)

    case GetNumberDialCode(number) =>
      val dialCodeCountry = dialCodes.last._2.par.filter(d => number.startsWith(d._1))
        .toList.sortBy(_._1.length).lastOption

      dialCodeCountry match {
        case Some(dt) => sender ! NumberDialCodeCountry(Some(dt._1), Some(dt._2))
        case None => sender ! NumberDialCodeCountry(None, None)
      }

    case x@DelDialCodeList(fileName) =>
      dialCodes.size match {
        case 1 => sender ! ApiReply(400, "We cannot remove the default list of DialCodes ")
        case _ =>
          val newMap = dialCodes.filterNot(_._1 == fileName)
          context become idle(newMap)
          sender ! ApiReply(200, s"DialCodes with filename $fileName, removed successfully")
      }

    case x@GetDialCodeList(fileName) =>
      val dialC = dialCodes.get(fileName)
      //log info s"------->  $dialC"
      dialC match {
        case Some(map) => sender ! dialC
        case None => sender ! None
      }

    case x@GetAllDialCodeList =>
      sender ! dialCodes.keys
  }

  def receive: Receive =
    idle(dialCodes)
}
