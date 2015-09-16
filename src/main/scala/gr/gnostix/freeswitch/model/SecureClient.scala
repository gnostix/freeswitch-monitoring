package gr.gnostix.freeswitch.model

import java.util.Date

import org.atmosphere.cpr.{AtmosphereResource, AtmosphereResourceFactory}
import org.scalatra.atmosphere._
import scala.concurrent.ExecutionContext.Implicits.global
/**
 * Created by rebel on 13/9/15. */

class SecureClient extends AtmosphereClient {

  // adminUuids is a collection of uuids for admin users. You'd need to
  // add each admin user's uuid to the list at connection time.
  var adminUuids: List[String] = List()

//  final protected def OnlyAdmins: ClientFilter = adminUuids.contains(_)



  val adm = new OnlyAdmins()

  override def receive: AtmoReceive = {
    case Connected =>
      println("Secure Client %s is connected" format uuid)
      adm.addUuid(uuid)

    case Disconnected(disconnector, Some(error)) =>
      println("Secure Disconnected ----error-----" + error + " disconnector: " + disconnector)

    case x@Disconnected(ClientDisconnected, _) =>
      println("Disconnected(ClientDisconnected, _) | Client %s is disconnected" format uuid)
      //broadcast(("author" -> "Someone") ~ ("message" -> "has left the room") ~ ("time" -> (new Date().getTime.toString)), Everyone)

    case x @ JsonMessage(json) =>
      println("-----> message received on events " + json.toString)
      adminBroadcast(x) // by default a broadcast is to everyone but self
  }

  def adminBroadcast(msg: JsonMessage) {
    broadcast(msg, adm)
  }

}

class OnlyAdmins extends ClientFilter(null) {
  var uuidsList: List[String] = List()

  def addUuid(newUuid: String) = uuidsList ::= newUuid

  def removeUuid(newUuid: String) = uuidsList = uuidsList.filterNot(_ == newUuid)

  def apply(v1: AtmosphereResource): Boolean = uuidsList contains  v1.uuid()

  override def toString(): String = "OnlyAdmins"
}
