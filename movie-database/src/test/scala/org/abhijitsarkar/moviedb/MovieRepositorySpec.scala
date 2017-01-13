package org.abhijitsarkar.moviedb

import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * @author Abhijit Sarkar
  */
class MovieRepositorySpec extends FlatSpec
  with Matchers
  with MovieRepository
  with MovieRepositoryHelper {

  "MovieRepository" should "create a movie" in {
    val id = createMovie(Movie("test", -1, -1.0, "1"))

    Await.result(id, 1.second) shouldBe Some("1")
  }

  "MovieRepository" should "find a movie" in {
    val created = Await.result(createMovie(Movie("test", -1, -1.0, "1")), 1.second).getOrElse("")

    val found = findById(created)

    Await.result(found, 1.second) shouldBe Some("1")
  }

  "MovieRepository" should "delete a movie" in {
    val created = Await.result(createMovie(Movie("test", -1, -1.0, "1")), 1.second).getOrElse("")

    val found = findById(created)

    Await.result(found, 1.second) shouldBe Some("1")

    val deleted = Await.result(deleteMovie(created), 1.second).getOrElse("")

    val notFound = findById(deleted)

    Await.result(notFound, 1.second) shouldBe None
  }
}
