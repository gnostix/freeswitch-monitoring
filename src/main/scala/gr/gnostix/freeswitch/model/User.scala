package gr.gnostix.freeswitch.model

import org.slf4j.LoggerFactory

/**
 * Created by rebel on 6/9/15.
 */
case class User(userId: Int, username: String, var password: String)

object UserDao {

  val logger = LoggerFactory.getLogger(getClass)

  def getUserByUsername(username: String): Option[User] = {
    if (username == "admin"){
      Some(User(1, "admin", "admin"))
    } else {
      None
    }
  }

  def getUserById(userId: Int): Option[User] = {
    if (userId == 1){
      Some(User(1, "admin", "admin"))
    } else {
      None
    }
  }

}
