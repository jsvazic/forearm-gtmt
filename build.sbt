name := "Forearm"

version := "1.0"

organization := "com.arm"

scalaVersion := "2.10.2"

scalacOptions ++= List(
  "-deprecation",
  "-feature"
)

libraryDependencies ++= List(
  "org.slf4j" % "slf4j-api" % "1.7.6",
  "ch.qos.logback" % "logback-classic" % "1.1.1",
  "ch.qos.logback" % "logback-core" % "1.1.1",
  "ch.qos.logback" % "logback-access" % "1.1.1",
  "org.quartz-scheduler" % "quartz" % "2.2.1",
  "joda-time" % "joda-time" % "2.3",
  "org.joda" % "joda-convert" % "1.6",
  "io.argonaut" % "argonaut_2.10" % "6.0.2",
  "net.databinder.dispatch" % "dispatch-core_2.10" % "0.11.0"
)