import org.scalatra.atmosphere.{OutboundMessage, ClientFilter, AtmosphereClient}
import scala.concurrent.ExecutionContext.Implicits.global
import org.atmosphere.cpr.AtmosphereResource

/*
class SecureClient extends AtmosphereClient {


  var adminUuids: List[String] = List()
  // adminUuids is a collection of uuids for admin users. You'd need to
  // add each admin user's uuid to the list at connection time.
  final protected def OnlyAdmins: ClientFilter = adminUuids.contains(_.uuid)

  /**
   * Broadcast a message to admin users only.
   */
  def adminBroadcast(msg: OutboundMessage) {
    broadcast(msg, OnlyAdmins)
  }
}*/
