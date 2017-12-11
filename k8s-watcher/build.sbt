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
//lazy val akkaPersistenceMongoVersion = "2.0.4"

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
//  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
//  "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "org.typelevel" %% "cats-core" % catsVersion,
  "org.reactivemongo" %% "reactivemongo-akkastream" % reactivemongoVersion,
  "org.reactivemongo" %% "reactivemongo-bson-macros" % reactivemongoVersion,
  "org.reactivemongo" %% "reactivemongo" % reactivemongoVersion,
//  "com.github.scullxbones" %% "akka-persistence-mongo-rxmongo" % akkaPersistenceMongoVersion,
  "com.github.pureconfig" %% "pureconfig" % pureconfigVersion,
  "de.flapdoodle.embed" % "de.flapdoodle.embed.mongo" % embeddedMongoVersion,
  "org.scalatest" %% "scalatest" % scalatestVersion % Test,
  "com.github.tomakehurst" % "wiremock" % wiremockVersion % Test,
  "org.pegdown" % "pegdown" % pegdownVersion % Test,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  "ch.qos.logback" % "logback-classic" % logbackVersion % Runtime
)

fork in run := true

import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}

lazy val dockerSettings = Seq(
  dockerAlias := DockerAlias(dockerRepository.value, None, name.value,
    Some((version in Docker).value)),
  assemblyMergeStrategy in assembly := {
//    case "reference.conf" => MergeStrategy.last
    case x => {
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      val strategy = oldStrategy(x)
      if (strategy == MergeStrategy.deduplicate)
        MergeStrategy.first
      else strategy
    }
  },

  // Remove all jar mappings in universal and append the fat jar
  mappings in Universal := {
    val universalMappings = (mappings in Universal).value
    val fatJar = (assembly in Compile).value
    val filtered = universalMappings.filter {
      case (file, name) => !name.endsWith(".jar")
    }
    filtered :+ (fatJar -> ("lib/" + fatJar.getName))
  },
  dockerRepository := Some("asarkar"),
  dockerAlias := DockerAlias(dockerRepository.value, None, name.value, None),
  dockerCommands := Seq(
    Cmd("FROM", "openjdk:8u151"),
    Cmd("WORKDIR", "/"),
    Cmd("COPY", "opt/docker/lib/*.jar", "/app.jar"),
    Cmd("RUN", "sh", "-c", "'touch /app.jar'"),
    ExecCmd("ENTRYPOINT", "sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -jar /app.jar")
  )
)

lazy val `k8s-watcher` = (project in file("."))
  .settings(dockerSettings)
  .enablePlugins(JavaAppPackaging)
