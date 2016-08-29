name := """user-service"""

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.11.8"

scalacOptions ++= Seq(
  "-feature", "-unchecked", "-deprecation", "-encoding", "utf8"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" % "akka-actor_2.11" % "2.4.9",
  "com.typesafe.akka" % "akka-slf4j_2.11" % "2.4.9",
  "com.typesafe.akka" % "akka-http-core_2.11" % "2.4.9",
  "com.typesafe.akka" % "akka-http-spray-json-experimental_2.11" % "2.4.9",
  "org.mongodb" % "casbah-core_2.11" % "3.1.1",
  "com.typesafe.slick" % "slick_2.11" % "3.1.1",
  "mysql" % "mysql-connector-java" % "5.1.36" % "runtime",
  "org.apache.commons" % "commons-dbcp2" % "2.1.1",
  "org.apache.commons" % "commons-pool2" % "2.4.2",
  "ch.qos.logback" % "logback-core" % "1.1.7",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "org.scalatest" % "scalatest_2.11" % "3.0.0" % "test",
  "com.typesafe.akka" % "akka-testkit_2.11" % "2.4.9" % "test",
  "com.typesafe.akka" % "akka-http-testkit_2.11" % "2.4.9" % "test"
)
