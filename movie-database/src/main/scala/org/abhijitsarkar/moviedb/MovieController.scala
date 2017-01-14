package org.abhijitsarkar.moviedb

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Sink, Source}
import cats.data.EitherT
import cats.instances.future._
import cats.instances.option._
import org.abhijitsarkar.moviedb.MovieProtocol._
import org.abhijitsarkar.moviedb.MovieService._

/**
  * @author Abhijit Sarkar
  */
object MovieController {
  val routes = (repo: MovieRepository, client: MovieClient) => {
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
                    case Some(m) => persistMovie(repo).runWith(Source.single(Right(m)), Sink.ignore); m
                    case _ => NotFound -> s"No movie found with id: $id"
                  }
                }
              }
            }
        }
      }
    }
  }
}
