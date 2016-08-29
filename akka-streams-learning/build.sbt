lazy val commonSettings = Seq(
  organization := "name.abhijitsarkar.scala",
  version := "1.0.0-SNAPSHOT",
  scalaVersion := "2.11.8",
  scalacOptions := Seq(
    "-feature", "-unchecked", "-deprecation", "-encoding", "utf8"
  ),
  libraryDependencies ++= Seq(
    "com.typesafe.akka" % "akka-stream_2.11" % "2.4.9",
    "org.apache.commons" % "commons-compress" % "1.12",
    "org.scalatest" % "scalatest_2.11" % "3.0.0" % "test",
    "com.typesafe.akka" % "akka-testkit_2.11" % "2.4.9" % "test",
    "com.typesafe.akka" % "akka-stream-testkit_2.11" % "2.4.9" % "test"
  )
)

lazy val `weather-streaming` = project.
  settings(commonSettings: _*)

lazy val `flight-streaming` = project.
  settings(commonSettings: _*)

lazy val `random-examples` = project.
  settings(commonSettings: _*)

lazy val `fibonacci-pub-sub` = project.
  settings(commonSettings: _*)

lazy val `meetup-streaming` = project.
  settings(commonSettings: _*).
  settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" % "akka-slf4j_2.11" % "2.4.9",
      "com.typesafe.akka" % "akka-http-core_2.11" % "2.4.9",
      "com.typesafe.akka" % "akka-http-spray-json-experimental_2.11" % "2.4.9",
      "ch.qos.logback" % "logback-core" % "1.1.7",
      "ch.qos.logback" % "logback-classic" % "1.1.7",
      "com.typesafe.akka" % "akka-http-testkit_2.11" % "2.4.9" % "test"
    )
  )

lazy val `akka-streams-learning` = (project in file(".")).
  settings(commonSettings: _*).
  aggregate(
    `weather-streaming`, `flight-streaming`,
    `random-examples`, `fibonacci-pub-sub`,
    `meetup-streaming`
  )

fork in run := true
