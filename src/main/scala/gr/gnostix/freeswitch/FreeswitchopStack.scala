package gr.gnostix.freeswitch

import org.scalatra._
import org.scalatra.scalate.ScalateSupport
import org.slf4j.LoggerFactory

trait FreeswitchopStack extends ScalatraServlet with ScalateSupport {

 implicit val logger =  LoggerFactory.getLogger(getClass)

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
