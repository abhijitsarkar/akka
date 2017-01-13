package org.abhijitsarkar.moviedb

import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.{FailoverStrategy, MongoConnectionOptions, MongoDriver}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * @author Abhijit Sarkar
  */
class MovieRepositorySpec extends FlatSpec
  with Matchers
  with BeforeAndAfterAll
  with MovieRepository {
  implicit val executor = ExecutionContext.global

  val opts = MongoConnectionOptions(
    failoverStrategy = FailoverStrategy(retries = 2)
  )

  override val movieCollection: Future[BSONCollection] = {
    MongoDriver().connection(List("localhost"), opts)
      .database("local")
      .map(_.collection("movie"))
  }

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

    val notFound = findById(created)

    Await.result(notFound, 1.second) shouldBe None
  }
}
