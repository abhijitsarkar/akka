package name.abhijitsarkar.scauth.service

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, ResponseEntity}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.scaladsl.{Sink, Source}
import name.abhijitsarkar.scauth.api.OAuthService
import name.abhijitsarkar.scauth.util.ActorPlumbing
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.util.{Failure, Success}

class SimpleOAuthService[A]()(implicit val actorPlumbing: ActorPlumbing) extends OAuthService[A] {
  private val log = LoggerFactory.getLogger(getClass())

  protected def sendAndReceive(httpRequest: HttpRequest, id: String)(implicit unmarshaller: Unmarshaller[ResponseEntity, A]) = {
    log.debug(s"Http request: {}.", httpRequest)

    import actorPlumbing._

    val pool = Http().superPool[String]()

    val response = Source.single(httpRequest -> id)
      .via(pool)
      .runWith(Sink.head)

    response.flatMap { r =>
      r._1 match {
        case Success(results) => Unmarshal(results.entity).to[A]
        case Failure(ex) => Future.failed(ex)
      }
    }
  }
}