package gr.gnostix.freeswitch.servlets

import gr.gnostix.freeswitch.FreeswitchopStack

class CentralServlet extends FreeswitchopStack {

  get("/") {
    <html>
      <body>
        <h1>Hello, world!</h1>
        Say <a href="hello-scalate">hello to Scalate</a>.
      </body>
    </html>
  }

}
