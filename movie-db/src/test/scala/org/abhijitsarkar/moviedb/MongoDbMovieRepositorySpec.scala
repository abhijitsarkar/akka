package org.abhijitsarkar.moviedb

import org.scalatest.{FlatSpec, Matchers}
import reactivemongo.api.MongoDriver

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * @author Abhijit Sarkar
  */
class MongoDbMovieRepositorySpec extends FlatSpec
  with Matchers {

  import TestHelper._

  val repo = MongoDbMovieRepository(MongoDriver())
  val timeout = 5.seconds

  "MovieRepository" should "create a movie" in {
    val id = repo.create(movies)

    Await.result(id, timeout) shouldBe 1
  }

  it should "find a movie" in {
    Await.result(repo.create(movies), timeout)

    val found = repo.findById("1")

    Await.result(found, timeout).map(_.imdbId) shouldBe Some("1")
  }

  it should "delete a movie" in {
    Await.result(repo.create(movies), timeout)

    val found = repo.findById("1")

    Await.result(found, timeout).map(_.imdbId) shouldBe Some("1")

    val deleted = Await.result(repo.delete("1"), timeout).getOrElse("")

    val notFound = repo.findById(deleted)

    Await.result(notFound, timeout) shouldBe None
  }
}
