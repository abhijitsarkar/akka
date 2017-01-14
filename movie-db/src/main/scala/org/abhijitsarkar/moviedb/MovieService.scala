package org.abhijitsarkar.moviedb

import akka.stream.scaladsl.Flow

/**
  * @author Abhijit Sarkar
  */
trait MovieService {
  def findMovieById(id: String) = (repo: MovieRepository) => repo.findById(id)

  def deleteMovie(id: String) = (repo: MovieRepository) => repo.delete(id)

  def persistMovies = (repo: MovieRepository) => Flow[Either[String, Movie]]
    .filter(_.isRight)
    .map(_.right.get)
    .grouped(50)
    .mapAsyncUnordered(16)(repo.create)

  def findMovieByTitleAndYear = (client: MovieClient) => Flow[(String, String)]
    .mapAsyncUnordered(16)(x => (client.findByTitleAndYear _).tupled(x))

  def findMovieByImdbId(id: String) = (client: MovieClient) => client.findById(id)
}
