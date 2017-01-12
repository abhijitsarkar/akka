package org.abhijitsarkar.moviedb

import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocumentReader, BSONDocumentWriter, Macros, document}

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Abhijit Sarkar
  */
trait MovieRepository {
  implicit val executor: ExecutionContext

  def movieCollection: Future[BSONCollection]

  implicit def personWriter: BSONDocumentWriter[Movie] = Macros.writer[Movie]

  implicit def personReader: BSONDocumentReader[Movie] = Macros.reader[Movie]

  def createMovie(movie: Movie): Future[Option[String]] = {
    val future = movieCollection.flatMap(_.insert(movie))

    for {
      result <- future
      either = result.ok match {
        case true => Some(movie.imdbId)
        case _ => None
      }
    } yield either
  }

  def deleteMovie(id: String): Future[Option[String]] = {
    val future = movieCollection.flatMap(_.remove(document("imdbId" -> id)))

    for {
      result <- future
      either = result.ok match {
        case true => Some(id)
        case _ => None
      }
    } yield either
  }

  def findById(id: String): Future[Option[String]] = {
    val result: Future[Option[Movie]] = movieCollection
      .flatMap(_.find(document("imdbId" -> id)).one[Movie])

    result.map {
      case x@Some(_) => x.map(_.imdbId)
      case _ => None
    }
  }
}
