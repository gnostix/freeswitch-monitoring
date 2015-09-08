import sbt._
import Keys._
import org.scalatra.sbt._
import org.scalatra.sbt.PluginKeys._
import com.mojolly.scalate.ScalatePlugin._
import ScalateKeys._

object FreeswitchopBuild extends Build {
  val Organization = "gr.gnostix"
  val Name = "FreeswitchOP"
  val Version = "0.1.0-SNAPSHOT"
  val ScalaVersion = "2.11.5"
  val ScalatraVersion = "2.3.0"
  val json4sversion = "3.2.10"
  val jettyVersion = "9.2.1.v20140609" //9.2.10.v20150310"


  lazy val project = Project(
    "freeswitchop",
    file("."),
    settings = ScalatraPlugin.scalatraSettings ++ scalateSettings ++ Seq(
      organization := Organization,
      name := Name,
      version := Version,
      scalaVersion := ScalaVersion,
      resolvers += Classpaths.typesafeReleases,
      resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases",
      ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) },
      scalacOptions in ThisBuild ++= Seq("-feature"),
      libraryDependencies ++= Seq(
        "org.scalatra" %% "scalatra" % ScalatraVersion,
        "org.scalatra" %% "scalatra-auth" % ScalatraVersion,
        "org.scalatra" %% "scalatra-scalate" % ScalatraVersion,
        "org.scalatra" %% "scalatra-specs2" % ScalatraVersion % "test",
        "org.scalatra" %% "scalatra-scalatest" % ScalatraVersion % "test",
        "org.scalatra" %% "scalatra-atmosphere" % ScalatraVersion,
        "org.json4s" %% "json4s-jackson" % json4sversion,
        "org.eclipse.jetty" % "jetty-webapp" % jettyVersion % "container",
        "org.eclipse.jetty" % "jetty-plus" % jettyVersion % "container;provided",
        "org.eclipse.jetty.websocket" % "websocket-server" % jettyVersion % "container;provided",
        "javax.servlet" % "javax.servlet-api" % "3.1.0" % "container;provided;test" artifacts Artifact("javax.servlet-api", "jar", "jar"),
        "com.typesafe.akka" %% "akka-actor" % "2.3.12",
        "com.typesafe.akka" %% "akka-slf4j" % "2.3.12",
        "com.typesafe.akka" %% "akka-testkit" % "2.3.12",
        "org.scalatest" % "scalatest_2.11" % "2.2.5" % "test",
        "net.databinder.dispatch" %% "dispatch-core" % "0.11.3",
        "org.freeswitch.esl.client" % "org.freeswitch.esl.client" % "0.9.2",
        "ch.qos.logback" % "logback-classic" % "1.1.2" % "runtime",
        "javax.mail" % "mail" % "1.4.1"
      ),
  scalateTemplateConfig in Compile <<= (sourceDirectory in Compile) { base =>
    Seq(
      TemplateConfig(
        base / "webapp" / "WEB-INF" / "templates",
        Seq.empty, /* default imports should be added here */
        Seq(
          Binding("context", "_root_.org.scalatra.scalate.ScalatraRenderContext", importMembers = true, isImplicit = true)
        ), /* add extra bindings here */
        Some("templates")
      )
    )
  }
  )
  )
}
