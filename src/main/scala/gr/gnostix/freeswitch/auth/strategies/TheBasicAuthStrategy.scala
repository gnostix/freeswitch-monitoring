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

package gr.gnostix.api.auth.strategies


import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import gr.gnostix.freeswitch.model.{UserDao, User}
import org.scalatra.ScalatraBase
import org.scalatra.auth.strategy.BasicAuthStrategy
import org.slf4j.LoggerFactory


class TheBasicAuthStrategy(protected override val app: ScalatraBase, realm: String)
  extends BasicAuthStrategy[User](app, realm) {

  override def name: String = "TheBasicAuth"

  val logger = LoggerFactory.getLogger(getClass)

  override protected def getUserId(user: User)(implicit request: HttpServletRequest, response: HttpServletResponse): String = user.userId.toString

  override def isValid(implicit request: HttpServletRequest) = {
    logger.info("-----------> TheBasicAuthStrategy: isValid " + app.request.isBasicAuth +" " + app.request.providesAuth)
      app.request.isBasicAuth && app.request.providesAuth
  }

  override protected def validate(userName: String, password: String)(implicit request: HttpServletRequest, response: HttpServletResponse): Option[User] = {
    logger.info("TheBasicAuthStrategy: found the username in DB. userName: " + userName)
    UserDao.getUserByUsername(userName) match {
      case Some(user) => {
        logger.info("TheBasicAuthStrategy: found the username in DB")
        if (true) {
          Some(user)
        } else {
          logger.info("-----------> TheBasicAuthStrategy: login failed --> user and pass did not match!!");
          None
        }
      }
      case None => {
        logger.info("-----------> TheBasicAuthStrategy: login failed")
        None
      }
    }
  }

  /**
   * What should happen if the user is currently not authenticated?
   */
  override def unauthenticated()(implicit request: HttpServletRequest, response: HttpServletResponse) {
    //app.redirect("/sessions/new")
    logger.info("---------> TheBasicAuthStrategy: login unauthenticated, was redirected")
    //app.redirect("/login")
  }
}
