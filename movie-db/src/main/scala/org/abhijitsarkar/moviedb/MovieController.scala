package org.abhijitsarkar.moviedb

import java.net.URL

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Keep, Sink, Source}
import cats.data.EitherT
import cats.instances.future._
import cats.instances.option._
import org.abhijitsarkar.moviedb.ExcelMovieParser.parseMovies
import org.abhijitsarkar.moviedb.MovieProtocol._
import reactivemongo.api.MongoDriver

import scala.util.{Failure, Try}

/**
  * @author Abhijit Sarkar
  */
trait MovieController extends MovieService {
  val repo = MongoDbMovieRepository(MongoDriver())
  val client = OMDbClient()

  val routes = {
    logRequestResult(getClass.getSimpleName) {
      pathPrefix("movies") {
        path(Segment) { id =>
          get {
            complete {
              findMovieById(id)(repo).map[ToResponseMarshallable] {
                _ match {
                  case x@Some(_) => x
                  case _ => NotFound -> s"No movie found with id: $id"
                }
              }
            }
          } ~
            delete {
              complete {
                deleteMovie(id)(repo).map[ToResponseMarshallable] {
                  _ match {
                    case x@Some(_) => x
                    case _ => NotFound -> s"No movie found with id: $id"
                  }
                }
              }
            } ~
            put {
              complete {
                val f = EitherT(findMovieByImdbId(id)(client))

                f.to[Option].map[ToResponseMarshallable] {
                  _ match {
                    case Some(m) => persistMovies(repo).runWith(Source.single(Right(m)), Sink.ignore); m
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
                  .via(findMovieByTitleAndYear(client))
                  .via(persistMovies(repo))
                  .toMat(Sink.head)(Keep.right)
                  .run
                  .transform((n: Int) => OK -> s"Created $n movies", t => {
                    logger.error("Failed to create movies", t)
                    t
                  })
              }
              case Failure(t) => logger.error("Bad URL", t); BadRequest -> "Bad URL"
            }
          }
        }
      }
    }
  }
}
