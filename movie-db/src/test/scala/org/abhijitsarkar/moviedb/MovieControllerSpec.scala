package org.abhijitsarkar.moviedb

import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.ResponseEntity
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.http.scaladsl.util.FastFuture
import org.abhijitsarkar.moviedb.TestHelper._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers}
import spray.json._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

/**
  * @author Abhijit Sarkar
  */
class MovieControllerSpec extends FlatSpec
  with Matchers
  with MockFactory
  with ScalatestRouteTest
  with MovieController {
  // def screws up mocking, don't know why
  override val repo: MovieRepository = stub[MovieRepository]
  override val client: MovieClient = stub[MovieClient]

  object MovieJsonProtocol extends DefaultJsonProtocol {
    implicit val colorFormat = jsonFormat14(Movie)
  }

  import MovieJsonProtocol._

  private implicit val movieUnmarshaller = Unmarshaller[ResponseEntity, Movie](ec => r => {
    val str = r.dataBytes.runFold("")((u, b) => s"$u${b.utf8String}")

    str.map { s =>
      Try(s.parseJson.convertTo[Movie]) match {
        case Success(m) => m
        case Failure(t) => throw t
      }
    }
  })

  "MovieController" should "return 200 and the movie when it is found" in {
    (repo.findById _).when("id").returns(FastFuture.successful(Some(movie)))

    Get(s"/movies/id") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`

      val m = Await.result(Unmarshal(response.entity).to[Movie], 1.second)

      m.title shouldBe "test"
    }
  }

  it should "return 404 if the movie is not found" in {
    (repo.findById _).when("id").returns(FastFuture.successful(None))

    Get(s"/movies/id") ~> routes ~> check {
      status shouldBe NotFound

      val s = Await.result(Unmarshal(response.entity).to[String], 1.second)

      s shouldBe empty
    }
  }

  it should "return 204 after deleting a movie" in {
    (repo.delete _).when("id").returns(FastFuture.successful(Some("id")))

    Delete(s"/movies/id") ~> routes ~> check {
      status shouldBe NoContent

      val s = Await.result(Unmarshal(response.entity).to[String], 1.second)

      s shouldBe empty
    }
  }

  it should "return 404 if the movie is not found for deletion" in {
    (repo.delete _).when("id").returns(FastFuture.successful(None))

    Delete(s"/movies/id") ~> routes ~> check {
      status shouldBe NotFound
    }
  }

  it should "return 204 after updating a movie" in {
    (client.findById _).when("id").returns(FastFuture.successful(Right(movie)))
    (repo.findById _).when("id").returns(FastFuture.successful(Some(movie)))
    (repo.create _).when(*).onCall((m: Seq[Movie]) => FastFuture.successful(1))

    Put(s"/movies/id") ~> routes ~> check {
      status shouldBe NoContent

      val s = Await.result(Unmarshal(response.entity).to[String], 1.second)

      s shouldBe empty
    }
  }

  it should "return 500 if failed to update a movie" in {
    (client.findById _).when("id").returns(FastFuture.successful(Right(movie)))
    (repo.findById _).when("id").returns(FastFuture.successful(Some(movie)))
    (repo.create _).when(*).onCall((m: Seq[Movie]) => FastFuture.successful(0))

    Put(s"/movies/id") ~> routes ~> check {
      status shouldBe InternalServerError

      val s = Await.result(Unmarshal(response.entity).to[String], 1.second)

      s should not be empty
    }
  }

  it should "return 500 if the movie lookup fails for update" in {
    (client.findById _).when("id").returns(FastFuture.successful(Left("not found")))
    (repo.findById _).when("id").returns(FastFuture.successful(Some(movie)))

    Put(s"/movies/id") ~> routes ~> check {
      status shouldBe InternalServerError

      (repo.create _).verify(*).never

      val s = Await.result(Unmarshal(response.entity).to[String], 1.second)

      s should not be empty
    }
  }

  it should "return 201 after creating a new movie" in {
    (client.findById _).when("id").returns(FastFuture.successful(Right(movie)))
    (repo.findById _).when("id").returns(FastFuture.successful(None))
    (repo.create _).when(*).onCall((m: Seq[Movie]) => FastFuture.successful(m.size))

    Put(s"/movies/id") ~> routes ~> check {
      status shouldBe Created

      val s = Await.result(Unmarshal(response.entity).to[String], 1.second)

      s shouldBe empty
    }
  }

  it should "return 500 if failed to create a new movie" in {
    (repo.findById _).when("id").returns(FastFuture.successful(None))
    (client.findById _).when("id").returns(FastFuture.successful(Right(movie)))
    (repo.create _).when(*).onCall((m: Seq[Movie]) => FastFuture.successful(0))

    Put(s"/movies/id") ~> routes ~> check {
      status shouldBe InternalServerError

      val s = Await.result(Unmarshal(response.entity).to[String], 1.second)

      s should not be empty
    }
  }

  it should "return 500 if the movie lookup fails for insert" in {
    (client.findById _).when("id").returns(FastFuture.successful(Left("not found")))
    (repo.findById _).when("id").returns(FastFuture.successful(None))

    Put(s"/movies/id") ~> routes ~> check {
      status shouldBe InternalServerError

      (repo.create _).verify(*).never

      val s = Await.result(Unmarshal(response.entity).to[String], 1.second)

      s should not be empty
    }
  }

  it should "return 202 in response to bulk creation request" in {
    (client.findByTitleAndYear _).when(*, *).returns(FastFuture.successful(Right(movie)))
    (repo.create _).when(*).onCall((m: Seq[Movie]) => FastFuture.successful(m.size))

    Post(s"/movies", getClass.getResource("/test.xlsx").toString) ~> routes ~> check {
      status shouldBe Accepted
    }
  }

  it should "return 400 if the URL is malformed" in {
    Post(s"/movies", "whatever") ~> routes ~> check {
      status shouldBe BadRequest

      val s = Await.result(Unmarshal(response.entity).to[String], 1.second)

      s should not be empty
    }
  }
}
