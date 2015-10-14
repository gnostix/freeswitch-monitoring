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

import gr.gnostix.freeswitch.actors.ActorsProtocol.RouterProtocol
import org.json4s.{Extraction, NoTypeHints}
import org.json4s.jackson.Serialization
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.{read, write}
import scala.language.implicitConversions
import org.scalatra.atmosphere.JsonMessage


/**
 * Created by rebel on 12/8/15.
 */
object ActorsJsonProtocol {
  implicit val formats = Serialization.formats(NoTypeHints)


  def caseClassToJsonMessage(message: Any): JsonMessage =
    JsonMessage(Extraction.decompose(message))

  def callsTimeSeriesToJson(callsTimeSeries: BasicStatsCalls): JsonMessage =
  JsonMessage(Extraction.decompose(callsTimeSeries))

  def heartbeatToJson(heartBeat: HeartBeat): JsonMessage =
  JsonMessage(Extraction.decompose(heartBeat))

  implicit def newCallToJson(newCall: CallNew): JsonMessage =
  JsonMessage(Extraction.decompose(newCall))

  implicit def endCallToJson(endCall: CallEnd): JsonMessage =
  //  JsonMessage(parse(write(endCall)))
  JsonMessage(Extraction.decompose(endCall))

  implicit def failedCallToJson(failedCall: FailedCall): JsonMessage =
    JsonMessage(Extraction.decompose(failedCall))

  implicit def heartbeatToText(heartBeat: HeartBeat): String =
    //pretty(render(parse(write(heartBeat))))
    pretty(render(Extraction.decompose(heartBeat)))

}

