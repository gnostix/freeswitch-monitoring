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
