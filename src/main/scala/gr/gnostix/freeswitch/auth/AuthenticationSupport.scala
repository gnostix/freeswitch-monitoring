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

package gr.gnostix.api.auth


import gr.gnostix.api.auth.strategies.{TheBasicAuthStrategy, UserPasswordStrategy}
import gr.gnostix.freeswitch.model.{User, UserDao}
import org.scalatra.ScalatraBase
import org.scalatra.auth.{ScentryConfig, ScentrySupport}

//import com.constructiveproof.hackertracker.auth.strategies.RememberMeStrategy

import org.slf4j.LoggerFactory

trait AuthenticationSupport extends ScalatraBase with ScentrySupport[User] {
  self: ScalatraBase =>

  val realm = "FS-moni Basic Auth"
  val log = LoggerFactory.getLogger(getClass)


  protected val scentryConfig = (new ScentryConfig {}).asInstanceOf[ScentryConfiguration]


  protected def fromSession = {
    case userId: String => {
      log.info("----> get from SessionStore")
      //User(name,98)
      UserDao.getUserById(userId.toInt).get
    }
  }

  protected def toSession = {
    case user: User => {
      log.info("-----> store to SessionStore")
      user.userId.toString
    }
  }

  protected def requireLogin() = {
    if (!isAuthenticated) {
      log.info("------------------> trait:requiredLogin1: not authenticated")
      //halt(401)
      //redirect("/login")
      scentry.authenticate()
      if (!isAuthenticated) {
        log.info("------------------> trait:requiredLogin2: not authenticated")
        halt(401)
      }
    }
  }

  override protected def configureScentry = {
    scentry.unauthenticated {
      scentry.strategies("UserPassword").unauthenticated()
    }
  }

  override protected def registerAuthStrategies = {
    scentry.register("TheBasicAuth", app => new TheBasicAuthStrategy(app, realm))
    scentry.register("UserPassword", app => new UserPasswordStrategy(app))
    //scentry.register("RememberMe", app => new RememberMeStrategy(app))
  }

}