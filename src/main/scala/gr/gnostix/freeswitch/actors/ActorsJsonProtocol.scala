package gr.gnostix.freeswitch.actors

import org.json4s.NoTypeHints
import org.json4s.jackson.Serialization
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.{read, write}

import org.scalatra.atmosphere.JsonMessage


/**
 * Created by rebel on 12/8/15.
 */
object ActorsJsonProtocol {
  implicit val formats = Serialization.formats(NoTypeHints)

  implicit def heartbeatToJson(heartBeat: HeartBeat): JsonMessage =
    JsonMessage(parse(write(heartBeat)))

  //JsonMessage(Extraction.decompose(heartBeat))
  implicit def newCallToJson(newCall: CallNew): JsonMessage =
    JsonMessage(parse(write(newCall)))
  implicit def endCallToJson(endCall: CallEnd): JsonMessage =
    JsonMessage(parse(write(endCall)))
  implicit def heartbeatToText(heartBeat: HeartBeat): String =
    pretty(render(parse(write(heartBeat))))
  implicit def chatHeartToText(chatHeartBeat: ChatHeartBeat): JsonMessage =
    JsonMessage(parse(write(chatHeartBeat)))
}

