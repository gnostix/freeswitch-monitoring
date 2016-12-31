import org.scalatra.sbt._
import org.scalatra.sbt.PluginKeys._
import com.mojolly.scalate.ScalatePlugin._
import ScalateKeys._



ScalatraPlugin.scalatraSettings

scalateSettings

scalacOptions := Seq("-unchecked", "-feature", "-deprecation", "-encoding", "utf8")

organization := "gr.gnostix"

name := "FreeswitchOP"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.8"

resolvers += Classpaths.typesafeReleases

val ScalatraVersion = "2.5.0"

val json4sversion = "3.3.0"

val jettyVersion = "9.2.10.v20150310"

val akkaVersion = "2.4.16"

libraryDependencies ++= Seq(
  "org.scalatra" %% "scalatra" % ScalatraVersion,
  "org.scalatra" %% "scalatra-auth" % ScalatraVersion,
  "org.scalatra" %% "scalatra-scalate" % ScalatraVersion,
  "org.scalatra" %% "scalatra-specs2" % ScalatraVersion % "test",
  "org.scalatra" %% "scalatra-scalatest" % ScalatraVersion % "test",
  "org.scalatra" %% "scalatra-atmosphere" % ScalatraVersion, // exclude("org.atmosphere", "atmosphere-compat-tomcat"),
  "org.json4s" %% "json4s-jackson" % json4sversion,
  "org.eclipse.jetty" % "jetty-webapp" % jettyVersion % "container;compile",
  "org.eclipse.jetty" % "jetty-plus" % jettyVersion % "container;provided",
  "org.eclipse.jetty" % "jetty-servlets" % jettyVersion,
  "org.eclipse.jetty.websocket" % "websocket-server" % jettyVersion % "container;provided",
  "javax.servlet" % "javax.servlet-api" % "3.1.0" % "container;provided;test" artifacts Artifact("javax.servlet-api", "jar", "jar"),
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalatest" % "scalatest_2.11" % "2.2.5" % "test",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.3",
  "org.freeswitch.esl.client" % "org.freeswitch.esl.client" % "0.9.2",
  "ch.qos.logback" % "logback-classic" % "1.1.2" % "runtime",
  "javax.mail" % "mail" % "1.4.1",
  "joda-time" % "joda-time" % "2.9.7",
  "org.joda" % "joda-convert" % "1.8.1"
)

scalateTemplateConfig in Compile := {
  val base = (sourceDirectory in Compile).value
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

enablePlugins(JettyPlugin)