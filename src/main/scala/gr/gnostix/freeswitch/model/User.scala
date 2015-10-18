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

package gr.gnostix.freeswitch.model

import org.slf4j.LoggerFactory

/**
 * Created by rebel on 6/9/15.
 */
case class User(userId: Int, username: String, var password: String, firstName: String, lastName: String, company: String, role: String)

object UserDao {

  val logger = LoggerFactory.getLogger(getClass)

  def getUserByUsername(username: String): Option[User] = {
    if (username == "admin"){
      Some(User(1, "admin", "admin", "Alex", "Kapas", "Fs-Moni", "admin"))
    } else {
      None
    }
  }

  def getUserById(userId: Int): Option[User] = {
    if (userId == 1){
      Some(User(1, "admin", "admin", "Alex", "Kapas", "Fs-Moni", "admin"))
    } else {
      None
    }
  }

}
