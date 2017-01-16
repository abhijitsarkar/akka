package org.abhijitsarkar.moviedb

import akka.http.scaladsl.util.FastFuture
import reactivemongo.api._
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.Command
import reactivemongo.bson.{BSONArray, BSONDocument, BSONDocumentReader, BSONDocumentWriter, Macros, document}

import scala.concurrent.Future

/**
  * @author Abhijit Sarkar
  */
trait MovieRepository {
  def create(movies: Seq[Movie]): Future[Int]

  def delete(id: String): Future[Option[String]]

  def findById(id: String): Future[Option[Movie]]
}

class MongoDbMovieRepository(val movieCollection: Future[BSONCollection]) extends MovieRepository {
  private implicit def movieWriter: BSONDocumentWriter[Movie] = Macros.writer[Movie]

  private implicit def movieReader: BSONDocumentReader[Movie] = Macros.reader[Movie]

  override def create(movies: Seq[Movie]) = {
    movieCollection.flatMap { coll =>
      val bulkDocs = BSONArray(
        movies.map { movie =>
          BSONDocument("q" -> document("_id" -> movie.imdbId), "u" -> movieWriter.write(movie), "upsert" -> true)
        }
      )

      val doc = BSONDocument(
        "update" -> coll.name,
        "updates" -> bulkDocs,
        "ordered" -> false // continue on error
      )

      val runner = Command.run(BSONSerializationPack, FailoverStrategy())

      // returns {"n":10,"nModified":10,"ok":1}
      runner.apply(coll.db, runner.rawCommand(doc)).one[BSONDocument](ReadPreference.nearest)
        .map(d => d.getAs[Int]("n").getOrElse(0))
    }
  }

  override def delete(id: String) = {
    val future = movieCollection.flatMap(_.remove(document("_id" -> id)))

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
      .flatMap(_.find(document("_id" -> id)).one[Movie])
  }
}

object MongoDbMovieRepository {
  def apply(driver: MongoDriver) = new MongoDbMovieRepository(movieCollection(driver))

  private def movieCollection(driver: MongoDriver): Future[BSONCollection] = {
    val mongodbConfig = config.getConfig("mongodb")

    val db = for {
      uri <- Future.fromTry(MongoConnection.parseURI(mongodbConfig.getString("uri")))
      con = {
        logger.debug(s"Connecting to MongoDB URI: ${uri.toString}")
        driver.connection(uri)
      }
      dn <- FastFuture.successful(uri.db.get)
      db <- con.database(dn)
    } yield db

    db.map(_.collection(mongodbConfig.getString("collection")))
  }
}
