package org.abhijitsarkar.moviedb

import org.scalatest._

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * @author Abhijit Sarkar
  */
class OMDbClientSpec extends FlatSpec
  with Matchers
  with EitherValues {

  val client = OMDbClient()
  "Client" should "find a movie by title and year" in {
    val movie = Await.result(client.findByTitleAndYear("rogue one", "2016"), 1.second).right.value

    movie.imdbId should be("tt3748528")
  }

  "Client" should "find a movie by IMDB id" in {
    val movie = Await.result(client.findById("tt3748528"), 1.second).right.value

    movie.imdbId should be("tt3748528")
  }
}
