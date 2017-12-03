package org.abhijitsarkar.akka.k8s.watcher.repository

import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.{MongoConnection, MongoDriver}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Abhijit Sarkar
  */
object MongoClientHelper {
  val driver = MongoDriver()

  def mongoCollection(mongoProperties: MongoProperties)(implicit ec: ExecutionContext): Future[BSONCollection] = {
    val connection = (uri: String) => MongoConnection.parseURI(uri)
      .map(driver.connection)

    Future.fromTry(connection(mongoProperties.uri))
      .flatMap(_.database(mongoProperties.db))
      .map(_.collection(mongoProperties.collection))
  }

  def closeDriver() = driver.close(5.seconds)
}
