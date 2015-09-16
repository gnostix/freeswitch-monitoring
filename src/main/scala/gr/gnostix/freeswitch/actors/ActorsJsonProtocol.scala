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

