package org.abhijitsarkar.moviedb

import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.{FailoverStrategy, MongoConnectionOptions, MongoDriver}
import reactivemongo.bson.{BSONDocumentReader, BSONDocumentWriter, Macros, document}

import scala.concurrent.Future

/**
  * @author Abhijit Sarkar
  */
trait MovieRepository {
  def create(movie: Movie): Future[Option[String]]

  def delete(id: String): Future[Option[String]]

  def findById(id: String): Future[Option[Movie]]
}

class MongoDbMovieRepository(val movieCollection: Future[BSONCollection]) extends MovieRepository {
  private implicit def personWriter: BSONDocumentWriter[Movie] = Macros.writer[Movie]

  private implicit def personReader: BSONDocumentReader[Movie] = Macros.reader[Movie]

  override def create(movie: Movie) = {
    val future = movieCollection.flatMap(_.insert(movie))

    for {
      result <- future
      either = result.ok match {
        case true if (result.n > 0) => Some(movie.imdbId)
        case _ => None
      }
    } yield either
  }

  override def delete(id: String) = {
    val future = movieCollection.flatMap(_.remove(document("imdbId" -> id)))

    for {
      result <- future
      either = result.ok match {
        case true if (result.n > 0) => Some(id)
        case _ => None
      }
    } yield either
  }

  override def findById(id: String) = {
    movieCollection
      .flatMap(_.find(document("imdbId" -> id)).one[Movie])
  }
}

object MongoDbMovieRepository {
  def apply(driver: MongoDriver) = new MongoDbMovieRepository(movieCollection(driver))

  private val opts = MongoConnectionOptions(
    failoverStrategy = FailoverStrategy(retries = 2)
  )

  import scala.collection.JavaConverters._

  private def movieCollection(driver: MongoDriver): Future[BSONCollection] = {
    val mongodbConfig = config.getConfig("mongodb")
    driver.connection(mongodbConfig.getStringList("servers").asScala, opts)
      .database(mongodbConfig.getString("database"))
      .map(_.collection(mongodbConfig.getString("collection")))
  }
}
