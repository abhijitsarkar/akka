package org.abhijitsarkar.akka.k8s.watcher.repository

import java.net.URI

/**
  * @author Abhijit Sarkar
  */
case class MongoProperties(
                            embedded: Boolean = true,
                            uri: String = "mongodb://localhost:27017/db",
                            collection: String = "events"
                          ) {
  private val u = URI.create(uri)

  val host = u.getHost
  val port = u.getPort
  val db: String = u.getPath
    .split("/")
    .takeRight(1)
    .head
}
