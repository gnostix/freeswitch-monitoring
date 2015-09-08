package gr.gnostix.freeswitch.model

import org.slf4j.LoggerFactory

/**
 * Created by rebel on 6/9/15.
 */
case class User(userId: Int, username: String, var password: String)

object UserDao {

  val logger = LoggerFactory.getLogger(getClass)

  def getUserByUsername(username: String): Option[User] = {
    Some(User(1, "alex", "1234"))
  }

  def getUserById(userId: Int): Option[User] = {
    Some(User(1, "alex", "1234"))
  }

}
