package org.abhijitsarkar.moviedb

import akka.actor.ActorSystem
import akka.event.NoLogging
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
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

  "Client" should "return a single result in" in {
    val movie = Await.result(movieInfo("rogue one", 2016), 1.second).right.value

    movie.imdbId should be("tt3748528")
  }
}
