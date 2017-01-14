name := "movie-database"
organization := "org.abhijitsarkar"
version := "1.0.0-SNAPSHOT"
scalaVersion := "2.12.1"

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
  "-Xfatal-warnings"
)

libraryDependencies ++= {
  val akkaVersion = "2.4.16"
  val akkaHttpVersion = "10.0.1"
  val scalaTestVersion = "3.0.1"
  val reactiveMongoVersion = "0.12.1"
  val poiVersion = "3.15"
  val logbackVersion = "1.1.7"
  val akkaSlf4jVersion = "2.4.16"
  val catsVersion = "0.8.1"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion,
    "org.reactivemongo" %% "reactivemongo" % reactiveMongoVersion,
    "org.typelevel" %% "cats" % catsVersion,
    "org.apache.poi" % "poi-ooxml" % poiVersion,
    "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
    "com.typesafe.akka" %% "akka-slf4j" % akkaSlf4jVersion,
    "ch.qos.logback" % "logback-core" % logbackVersion % Runtime,
    "ch.qos.logback" % "logback-classic" % logbackVersion % Runtime
  )
}


fork in run := true