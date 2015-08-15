package gr.gnostix.freeswitch.servlets

import gr.gnostix.freeswitch.FreeswitchopStack
import org.scalatra.atmosphere.{ProtocolMessage, TextMessage, OutboundMessage, AtmosphereClient}
import scala.concurrent.ExecutionContext.Implicits.global

class CentralServlet extends FreeswitchopStack {

  get("/koko") {
    <html>
      <body>
        <h1>Hello, world!</h1>
        Say <a href="hello-scalate">hello to Scalate</a>.
      </body>
    </html>
  }

 /* get("/brd"){
    object Koko extends TextMessage("koko--------------->")

    AtmosphereClient.broadcast("/live/events", Koko)
}*/

}
