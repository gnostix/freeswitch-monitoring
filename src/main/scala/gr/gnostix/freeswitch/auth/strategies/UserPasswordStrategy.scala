package gr.gnostix.api.auth.strategies

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import gr.gnostix.freeswitch.model.{User, UserDao}
import gr.gnostix.freeswitch.utilities.HelperFunctions
import org.scalatra.ScalatraBase
import org.scalatra.auth.ScentryStrategy
import org.slf4j.LoggerFactory

class UserPasswordStrategy(protected val app: ScalatraBase)
                          (implicit request: HttpServletRequest, response: HttpServletResponse)
  extends ScentryStrategy[User] {

  //val db: Database
  override def name: String = "UserPassword"

  val logger = LoggerFactory.getLogger(getClass)


  private def username = app.params.getOrElse("username", "")

  private def password = app.params.getOrElse("password", "")

  /** *
    * Determine whether the strategy should be run for the current request.
    */
  override def isValid(implicit request: HttpServletRequest) = {
    logger.info("---------->  UserPasswordStrategy: determining isValid: " + (username != "" && password != "").toString())
    username != "" && password != ""
  }


  /**
   * In real life, this is where we'd consult our data store, asking it whether the user credentials matched
   * any existing user. Here, we'll just check for a known login/password combination and return a user if
   * it's found.
   */
  def authenticate()(implicit request: HttpServletRequest, response: HttpServletResponse): Option[User] = {
    logger.info("UserPasswordStrategy: attempting authentication")


    UserDao.getUserByUsername(username) match {
      case Some(user) => {
        logger.info("UserPasswordStrategy: found the username in DB")
/*
        if (checkUserPassword(username, password, user.password)) {
          Some(user)
        } else {
          logger.info("-----------> UserPasswordStrategy: login failed --> user and pass did not match!!");
          None
        }*/
        Some(user)
      }
      case None => {
        logger.info("-----------> UserPasswordStrategy: login failed");
        None
      }
    }
  }

  /**
   * What should happen if the user is currently not authenticated?
   */
  override def unauthenticated()(implicit request: HttpServletRequest, response: HttpServletResponse) {
    //app.redirect("/sessions/new")
    logger.info("---------> UserPasswordStrategy: login unauthenticated, was redirected")
    //app.redirect("/login")
  }

  private def checkUserPassword(username: String, password: String, userDbPassword: String): Boolean = {
    //    logger.info ("---------> UserPasswordStrategy checkUserPassword :"  + username + ":" + password + ":" )
    //    logger.info (s"---------> UserPasswordStrategy userDbPassword : $userDbPassword")
    //
    //    logger.info (s"---------> UserPasswordStrategy checkUserPassword :  " + (username.concat(password) ) )
    //    logger.info (s"---------> UserPasswordStrategy Password :  ${HelperFunctions.sha1Hash(username.concat(password)) }")


    if (HelperFunctions.sha1Hash(username + password) == userDbPassword) true else false
  }


}