package org.abhijitsarkar.moviedb

import java.io.File

import akka.actor.ActorSystem
import akka.event.NoLogging
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, EitherValues, FlatSpec, Matchers}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.{FailoverStrategy, MongoConnectionOptions, MongoDriver}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * @author Abhijit Sarkar
  */
class MovieServiceSpec extends FlatSpec
  with Matchers
  with EitherValues
  with BeforeAndAfterAll
  with MovieService {
  override def afterAll(): Unit = {
    val whatever = Await.result(system.terminate(), 3.second)
  }

  override def config = ConfigFactory.parseString(
    """
    akka {
      loglevel = "WARNING"
    }
    omdb {
      host = "www.omdbapi.com"
      port = 80
    }
    """
  )

  implicit val system = ActorSystem(getClass.getSimpleName)

  override val executor = system.dispatcher

  override val logger = NoLogging
  override val materializer = ActorMaterializer()

  val opts = MongoConnectionOptions(
    failoverStrategy = FailoverStrategy(retries = 2)
  )

  implicit val ec: ExecutionContext = executor

  override val movieCollection: Future[BSONCollection] = {
    MongoDriver().connection(List("localhost"), opts)
      .database("local")
      .map(_.collection("movie"))
  }

  "MovieService" should "extract name and year from file" in {
    val m = parseMovies(new File(getClass.getResource("/test.xlsx").toURI).getAbsolutePath)

    m should have size 10
    m should contain(("Pretty Woman", "1990"))
    m should contain(("Aladdin", "1992"))
  }
}
