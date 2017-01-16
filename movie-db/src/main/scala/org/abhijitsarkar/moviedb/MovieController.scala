package org.abhijitsarkar.moviedb

import java.net.URL

import akka.http.scaladsl.marshalling.{Marshal, Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpEntity, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorAttributes
import akka.stream.scaladsl.{Keep, Sink, Source}
import cats.Applicative
import cats.data.{EitherT, OptionT}
import cats.instances.future._
import org.abhijitsarkar.moviedb.ExcelMovieParser.parseMovies
import org.abhijitsarkar.moviedb.MovieProtocol._
import spray.json._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Try}

/**
  * @author Abhijit Sarkar
  */
trait MovieController extends MovieService {
  // (StatusCode, T) can be marshaled, if a ToEntityMarshaller[T] is available
  // http://doc.akka.io/docs/akka-http/current/scala/http/common/marshalling.html
  private def notFoundResponse(id: String) = Marshal(NotFound -> s"No movie found with id: $id").to[HttpResponse]

  implicit val movieMarshaller: ToEntityMarshaller[Movie] = Marshaller.oneOf(
    Marshaller.withFixedContentType(`application/json`) { m =>
      HttpEntity(`application/json`, m.toJson.compactPrint)
    })

  private def okResponse[A](a: A)(implicit m: ToEntityMarshaller[A]) = Marshal(OK -> a).to[HttpResponse]

  val routes = {
    logRequestResult(getClass.getSimpleName) {
      pathPrefix("movies") {
        path(Segment) { id =>
          get {
            complete {
              OptionT(findMovieById(id))
                .semiflatMap(okResponse(_))
                .getOrElseF(notFoundResponse(id))
            }
          } ~
            delete {
              complete {
                OptionT(deleteMovie(id))
                  .semiflatMap(okResponse(_))
                  .getOrElseF(notFoundResponse(id))
              }
            } ~
            put {
              complete {
                EitherT(findMovieByImdbId(id))
                  .semiflatMap { m =>
                    persistMovies.runWith(Source.single(Right(m)), Sink.ignore)
                    okResponse(m)
                  }
                  .getOrElseF(notFoundResponse(id))
              }
            }
        } ~ (post & entity(as[String])) { url =>
          complete {
            Try(new URL(url)) match {
              case scala.util.Success(u) => {
                val src = Source.fromIterator(() => parseMovies(u).iterator)

                src
                  .via(findMovieByTitleAndYear)
                  .via(persistMovies)
                  .completionTimeout(5.minutes)
                  .toMat(Sink.fold(Future(0))((acc, elem) => Applicative[Future].map2(acc, elem)(_ + _)))(Keep.right)
                  // http://doc.akka.io/docs/akka/current/scala/dispatchers.html
                  // http://blog.akka.io/streams/2016/07/06/threading-and-concurrency-in-akka-streams-explained
                  // http://doc.akka.io/docs/akka/current/scala/stream/stream-parallelism.html
                  .withAttributes(ActorAttributes.dispatcher("blocking-io-dispatcher"))
                  .run.flatten
                  .onComplete {
                    _ match {
                      case scala.util.Success(n) => logger.info(s"Created $n movies")
                      case Failure(t) => logger.error(t, "Failed to create movies")
                    }
                  }

                Accepted
              }
              case Failure(t) => logger.error(t, "Bad URL"); BadRequest -> "Bad URL"
            }
          }
        }
      }
    }
  }
}
