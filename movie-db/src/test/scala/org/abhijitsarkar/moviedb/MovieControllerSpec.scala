package org.abhijitsarkar.moviedb

import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.ResponseEntity
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import org.abhijitsarkar.moviedb.TestHelper._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers}
import spray.json._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
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

  "MovieController" should "return movie when found" in {
    (repo.findById _).when("id").returns(Future(Some(movie)))

    Get(s"/movies/id") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`

      val m = Await.result(Unmarshal(response.entity).to[Movie], 1.second)

      m.title shouldBe "test"
    }
  }

  "MovieController" should "indicate when movie is not found" in {
    (repo.findById _).when("id").returns(Future(None))

    Get(s"/movies/id") ~> routes ~> check {
      status shouldBe NotFound
    }
  }

  "MovieController" should "delete movie when found" in {
    (repo.delete _).when("id").returns(Future(Some("id")))

    Delete(s"/movies/id") ~> routes ~> check {
      status shouldBe OK

      val m = Await.result(Unmarshal(response.entity).to[String], 1.second)

      m shouldBe "id"
    }
  }

  "MovieController" should "indicate when movie cannot be deleted" in {
    (repo.delete _).when("id").returns(Future(None))

    Delete(s"/movies/id") ~> routes ~> check {
      status shouldBe NotFound
    }
  }

  "MovieController" should "create a new movie" in {
    (client.findById _).when("id").returns(Future(Right(movie)))
    (repo.create _).when(*).onCall((m: Seq[Movie]) => Future(m.size))

    Put(s"/movies/id") ~> routes ~> check {
      status shouldBe OK

      val m = Await.result(Unmarshal(response.entity).to[Movie], 1.second)

      m.title shouldBe "test"
    }
  }

  "MovieController" should "indicate when a movie cannot be created" in {
    (client.findById _).when("id").returns(Future(Left("not found")))

    Put(s"/movies/id") ~> routes ~> check {
      status shouldBe NotFound
    }
  }

  "MovieController" should "accept request for creating new movies" in {
    (client.findByTitleAndYear _).when(*, *).returns(Future(Right(movie)))
    (repo.create _).when(*).onCall((m: Seq[Movie]) => Future(m.size))

    Post(s"/movies", getClass.getResource("/test.xlsx").toString) ~> routes ~> check {
      status shouldBe Accepted
    }
  }

  "MovieController" should "indicate when movies cannot be created" in {
    Post(s"/movies", "whatever") ~> routes ~> check {
      status shouldBe BadRequest
    }
  }
}
