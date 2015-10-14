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

package gr.gnostix.freeswitch.servlets

import gr.gnostix.api.auth.AuthenticationSupport
import gr.gnostix.freeswitch.FreeswitchopStack
import org.json4s.{JValue, Formats, DefaultFormats}
import org.scalatra.{ScalatraServlet, CorsSupport}
import org.scalatra.json.JacksonJsonSupport

class LoginServlet
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
    logger.error("--------------> /login: wewewewewewewe: ")

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
    requireLogin() //??
    scentry.logout()
  }

  notFound {
    // remove content type in case it was set through an action
    contentType = null
    // Try to render a ScalateTemplate if no route matched
    findTemplate(requestPath) map { path =>
      contentType = "text/html"
      layoutTemplate(path)
    } orElse serveStaticResource() getOrElse resourceNotFound()
  }

}
