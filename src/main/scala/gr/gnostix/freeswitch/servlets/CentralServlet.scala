package gr.gnostix.freeswitch.servlets

import gr.gnostix.api.auth.AuthenticationSupport
import gr.gnostix.freeswitch.FreeswitchopStack
import org.json4s.{Formats, DefaultFormats}
import org.scalatra.{ScalatraServlet, CorsSupport}
import org.scalatra.json.JacksonJsonSupport

class CentralServlet
extends ScalatraServlet
with JacksonJsonSupport
with AuthenticationSupport
with CorsSupport
with FreeswitchopStack {

  options("/*") {
    response.setHeader("Access-Control-Allow-Headers", request.getHeader("Access-Control-Request-Headers"))
  }

  // Sets up automatic case class to JSON output serialization, required by
  // the JValueResult trait.
  protected implicit val jsonFormats: Formats = DefaultFormats

  before() {
    contentType = formats("json")
  }

  post("/login") {
    scentry.authenticate()
    if (isAuthenticated) {
      logger.info("--------------> /login: successful Id: " + user.userId)

      logger.info("--------------> /login: request.getRemoteAddr : " + request.getRemoteAddr)
      logger.info("--------------> /login: username : " + user.username)
      logger.info("--------------> /login: session.getid : " + session.getId)


      user.password = ""
      user
    } else {
      logger.info("-----------------------> /login: NOT successful")
      halt(401, "bad username or password")
    }
  }


  post("/logout") {
    //SqlUtils.logUserLogout(user.username, session.getId)
    scentry.logout()
  }



}
