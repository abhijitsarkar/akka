package org.abhijitsarkar.moviedb

import akka.http.scaladsl.util.FastFuture
import akka.stream.scaladsl.{Keep, Sink, Source}
import cats.Applicative
import cats.instances.future._
import org.abhijitsarkar.moviedb.TestHelper.movie
import org.scalamock.scalatest.MockFactory
import org.scalatest.{EitherValues, FlatSpec, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * @author Abhijit Sarkar
  */
class MovieServiceSpec extends FlatSpec
  with Matchers
  with MockFactory
  with EitherValues
  with MovieService {
  // def screws up mocking, don't know why
  override val repo: MovieRepository = stub[MovieRepository]

  override val client: MovieClient = stub[MovieClient]

  "MovieService" should "persist movies only if Right projection" in {
    (repo.create _).when(*).onCall((m: Seq[Movie]) => FastFuture.successful(m.size))

    val src = Source.fromIterator(() => Seq(Right(movie), Left("error"), Right(movie), Right(movie)).iterator)

    val count = src
      .via(persistMovies)
      .toMat(Sink.fold(FastFuture.successful(0))((acc, elem) => Applicative[Future].map2(acc, elem)(_ + _)))(Keep.right)
      .run.flatten

    Await.result(count, 1.second) shouldBe 3
  }

  it should "convert tuple to Movie" in {
    (client.findByTitleAndYear _).when(*, *).returns(FastFuture.successful(Right(movie)))

    val m: Future[Either[String, Movie]] = findMovieByTitleAndYear
      .runWith(Source.single(("error", "movie")), Sink.head)._2

    Await.result(m, 1.second).right.value.title shouldBe "test"
  }
}
