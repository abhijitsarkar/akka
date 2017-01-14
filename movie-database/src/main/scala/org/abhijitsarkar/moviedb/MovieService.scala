package org.abhijitsarkar.moviedb

import akka.stream.scaladsl.Flow

import scala.concurrent.Future

/**
  * @author Abhijit Sarkar
  */
object MovieService {
  def findMovieById(id: String) = (repo: MovieRepository) => repo.findById(id)

  def deleteMovie(id: String) = (repo: MovieRepository) => repo.delete(id)

  def persistMovie = (repo: MovieRepository) => Flow[Either[String, Movie]]
    .mapAsyncUnordered(5) {
      _ match {
        case Right(m) => logger.debug(s"Persisting movie with id: ${m.imdbId} and title: ${m.title}"); repo.create(m)
        case Left(msg) => logger.error(msg); Future.failed(new RuntimeException(msg))
      }
    }

  def findMovieByTitleAndYear = (client: MovieClient) => Flow[(String, String)]
    .mapAsyncUnordered(10)(x => (client.findByTitleAndYear _).tupled(x))

  def findMovieByImdbId(id: String) = (client: MovieClient) => client.findById(id)
}
