package org.abhijitsarkar.moviedb

import akka.stream.scaladsl.Flow

import scala.concurrent.Future

/**
  * @author Abhijit Sarkar
  */
trait MovieService extends MovieRepository with OMDbClient {
  def findMovie = Flow[(String, String)]
    .mapAsyncUnordered(10)(x => (movieInfo _).tupled(x))

  def persistMovie = Flow[Either[String, Movie]]
    .mapAsyncUnordered(5) {
      _ match {
        case Right(m) => logger.debug(s"Persisting movie with id: ${m.imdbId} and title: ${m.title}"); createMovie(m)
        case Left(msg) => logger.error(msg); Future.failed(new RuntimeException(msg))
      }
    }
}
