package name.abhijitsarkar.akka.util

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import scala.concurrent.ExecutionContext

case class ActorPlumbing()(implicit val system: ActorSystem,
  implicit val executionContext: ExecutionContext,
  implicit val materializer: ActorMaterializer)