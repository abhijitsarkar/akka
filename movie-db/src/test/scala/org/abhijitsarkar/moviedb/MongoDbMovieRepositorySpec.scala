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

  val movies = {
    Seq(
      Movie(
        "test",
        -1,
        Nil,
        "",
        "",
        Nil,
        Nil,
        Nil,
        Nil,
        "",
        "",
        -1,
        -1.0,
        "1"
      )
    )
  }

  val repo = MongoDbMovieRepository(MongoDriver())

  "MovieRepository" should "create a movie" in {
    val id = repo.create(movies)

    Await.result(id, 1.second) shouldBe 1
  }

  "MovieRepository" should "find a movie" in {
    Await.result(repo.create(movies), 1.second)

    val found = repo.findById("1")

    Await.result(found, 1.second).map(_.imdbId) shouldBe Some("1")
  }

  "MovieRepository" should "delete a movie" in {
    Await.result(repo.create(movies), 1.second)

    val found = repo.findById("1")

    Await.result(found, 1.second).map(_.imdbId) shouldBe Some("1")

    val deleted = Await.result(repo.delete("1"), 1.second).getOrElse("")

    val notFound = repo.findById(deleted)

    Await.result(notFound, 1.second) shouldBe None
  }
}
