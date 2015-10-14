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

import akka.actor.{Props, ActorRef, Actor, ActorLogging}
import gr.gnostix.freeswitch.actors.ActorsProtocol._
import gr.gnostix.freeswitch.actors.ServletProtocol.{ApiReplyError, ApiReplyData, ApiReply}

import scala.collection.SortedMap

/**
 * Created by rebel on 10/10/15.
 */

object DialCodesActor {
  def props(dialCodes: Map[String, SortedMap[String, String]]): Props =
    Props(new DialCodesActor(dialCodes: Map[String, SortedMap[String, String]]))
}

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
        case Some(dt) => sender ! NumberDialCodeCountry(number, Some(dt._1), Some(dt._2))
        case None => sender ! NumberDialCodeCountry(number, None, None)
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
