package org.abhijitsarkar.moviedb

import java.net.URL

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorAttributes
import akka.stream.scaladsl.{Keep, Sink, Source}
import cats.Applicative
import cats.data.EitherT
import cats.instances.future._
import cats.instances.option._
import org.abhijitsarkar.moviedb.ExcelMovieParser.parseMovies
import org.abhijitsarkar.moviedb.MovieProtocol._

import scala.concurrent.Future
import scala.util.{Failure, Try}

/**
  * @author Abhijit Sarkar
  */
trait MovieController extends MovieService {
  val routes = {
    logRequestResult(getClass.getSimpleName) {
      pathPrefix("movies") {
        path(Segment) { id =>
          get {
            complete {
              findMovieById(id).map[ToResponseMarshallable] {
                _ match {
                  case Some(x) => x
                  case _ => NotFound -> s"No movie found with id: $id"
                }
              }
            }
          } ~
            delete {
              complete {
                deleteMovie(id).map[ToResponseMarshallable] {
                  _ match {
                    case Some(x) => x
                    case _ => NotFound -> s"No movie found with id: $id"
                  }
                }
              }
            } ~
            put {
              complete {
                val f = EitherT(findMovieByImdbId(id))

                f.to[Option].map[ToResponseMarshallable] {
                  _ match {
                    case Some(m) => persistMovies.runWith(Source.single(Right(m)), Sink.ignore); m
                    case _ => NotFound -> s"No movie found with id: $id"
                  }
                }
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
