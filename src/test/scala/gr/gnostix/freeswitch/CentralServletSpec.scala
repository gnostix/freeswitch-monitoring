package gr.gnostix.freeswitch

import gr.gnostix.freeswitch.servlets.CentralServlet
import org.scalatra.test.specs2._

// For more on Specs2, see http://etorreborre.github.com/specs2/guide/org.specs2.guide.QuickStart.html
class CentralServletSpec extends ScalatraSpec { def is =
  "GET / on CentralServlet"                     ^
    "should return status 200"                  ! root200^
                                                end

  addServlet(classOf[CentralServlet], "/*")

  def root200 = get("/") {
    status must_== 200
  }

}
