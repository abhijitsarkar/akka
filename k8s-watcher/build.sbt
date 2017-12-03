name := "k8s-watcher"
organization := "org.abhijitsarkar.akka"
version := "1.0.0-SNAPSHOT"

scalaVersion := "2.12.4"

lazy val akkaVersion = "2.5.7"
lazy val akkaHttpVersion = "10.0.10"
lazy val macwireVersion = "2.3.0"
lazy val taggingVersion = "2.2.0"
lazy val catsVersion = "1.0.0-RC1"
lazy val scalatestVersion = "3.0.4"
lazy val logbackVersion = "1.2.3"
lazy val wiremockVersion = "2.12.0"
lazy val reactivemongoVersion = "0.12.7"
lazy val embeddedMongoVersion = "2.0.0"
lazy val pegdownVersion = "1.6.0"
lazy val pureconfigVersion = "0.8.0"

scalacOptions := Seq(
  "-encoding",
  "UTF-8",
  "-target:jvm-1.8",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-unused-import",
  "-Ywarn-dead-code",
  "-Ywarn-infer-any",
  "-Ywarn-numeric-widen",
  "-Xfatal-warnings",
  "-Ypartial-unification"
)

fork in test := true

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/test-reports")

libraryDependencies ++= Seq(
  "com.softwaremill.macwire" %% "macros" % macwireVersion,
  "com.softwaremill.common" %% "tagging" % taggingVersion,
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "org.typelevel" %% "cats-core" % catsVersion,
  "org.reactivemongo" %% "reactivemongo-akkastream" % reactivemongoVersion,
  "org.reactivemongo" %% "reactivemongo-bson-macros" % reactivemongoVersion,
  "org.reactivemongo" %% "reactivemongo" % reactivemongoVersion,
  "de.flapdoodle.embed" % "de.flapdoodle.embed.mongo" % embeddedMongoVersion,
  "com.github.pureconfig" %% "pureconfig" % pureconfigVersion,
  "org.scalatest" %% "scalatest" % scalatestVersion % Test,
  "com.github.tomakehurst" % "wiremock" % wiremockVersion % Test,
  "org.pegdown" % "pegdown" % pegdownVersion % Test,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  "ch.qos.logback" % "logback-classic" % logbackVersion % Runtime
)

fork in run := true
