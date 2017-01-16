package org.abhijitsarkar.moviedb

import java.net.URL

import akka.http.scaladsl.marshalling.{Marshal, Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, StatusCode}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorAttributes
import akka.stream.scaladsl.{Keep, Sink, Source}
import cats.data.{EitherT, OptionT}
import cats.implicits._
import org.abhijitsarkar.moviedb.ExcelMovieParser.parseMovies
import org.abhijitsarkar.moviedb.MovieProtocol._
import spray.json._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Try, Success => Successful}

/**
  * @author Abhijit Sarkar
  */
trait MovieController extends MovieService {
  // (StatusCode, T) can be marshaled, if a ToEntityMarshaller[T] is available
  // http://doc.akka.io/docs/akka-http/current/scala/http/common/marshalling.html
  private implicit val movieMarshaller: ToEntityMarshaller[Movie] = Marshaller.oneOf(
    Marshaller.withFixedContentType(`application/json`) { m =>
      HttpEntity(`application/json`, m.toJson.compactPrint)
    })

  private def persistMovie(m: Movie, successCode: StatusCode): Future[HttpResponse] = persistMovies
    .runWith(Source.single(Right(m)), Sink.head)
    ._2.flatten.transformWith {
    case Successful(i) if (i == 1) => FastFuture.successful(HttpResponse(status = successCode))
    case Failure(ex) => FastFuture.successful(HttpResponse(status = InternalServerError, entity = ex.getMessage))
  }

  private def transformResponse(e: EitherT[Future, String, Movie]): Future[HttpResponse] = {
    e.value.transform(_ match {
      case Right(m) => persistMovie(m, Created)
      case Left(msg) => FastFuture.successful(HttpResponse(status = InternalServerError, entity = msg))
    }, identity)
  }.flatten

  val routes = {
    logRequestResult(getClass.getSimpleName) {
      pathPrefix("movies") {
        path(Segment) { id =>
          get {
            complete {
              OptionT(findMovieById(id))
                .semiflatMap(m => Marshal(OK -> m).to[HttpResponse])
                .getOrElse(HttpResponse(status = NotFound))
            }
          } ~
            delete {
              complete {
                OptionT(deleteMovie(id))
                  .map[StatusCode](_ => NoContent)
                  .getOrElse(NotFound)
              }
            } ~
            put {
              complete {
                val m = EitherT(findMovieByImdbId(id))

                m
                  .semiflatMap(_ => findMovieById(id))
                  .semiflatMap {
                    case Some(x) => persistMovie(x, NoContent)
                    case _ => transformResponse(m)
                  }
                  .getOrElseF(transformResponse(m))
              }
            }
        } ~ (post & entity(as[String])) { url =>
          complete {
            Try(new URL(url)) match {
              case Successful(u) => {
                val src = Source.fromIterator(() => parseMovies(u).iterator)

                src
                  .via(findMovieByTitleAndYear)
                  .via(persistMovies)
                  .completionTimeout(5.minutes)
                  .toMat(Sink.fold(FastFuture.successful(0))((acc, elem) => (acc |@| elem).map(_ + _)))(Keep.right)
                  // http://doc.akka.io/docs/akka/current/scala/dispatchers.html
                  // http://blog.akka.io/streams/2016/07/06/threading-and-concurrency-in-akka-streams-explained
                  // http://doc.akka.io/docs/akka/current/scala/stream/stream-parallelism.html
                  .withAttributes(ActorAttributes.dispatcher("blocking-io-dispatcher"))
                  .run.flatten
                  .onComplete {
                    case Successful(n) => logger.info(s"Created $n movies")
                    case Failure(t) => logger.error(t, "Failed to create movies")
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
