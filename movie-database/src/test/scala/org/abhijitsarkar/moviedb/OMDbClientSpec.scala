package org.abhijitsarkar.moviedb

import org.scalatest._

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * @author Abhijit Sarkar
  */
class OMDbClientSpec extends FlatSpec
  with Matchers
  with EitherValues
  with BeforeAndAfterAll
  with OMDbClient {
  "Client" should "return a single result in" in {
    val movie = Await.result(movieInfo("rogue one", "2016"), 1.second).right.value

    movie.imdbId should be("tt3748528")
  }
}
