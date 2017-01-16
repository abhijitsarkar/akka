package org.abhijitsarkar

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.Config

import scala.concurrent.ExecutionContext

/**
  * @author Abhijit Sarkar
  */
package object moviedb {
  implicit val system = ActorSystem("MovieApp")
  implicit val executor: ExecutionContext = system.dispatcher
  implicit val materializer: Materializer = ActorMaterializer()

  lazy val config: Config = system.settings.config

  lazy val logger: LoggingAdapter = Logging.getLogger(system, this)
}
