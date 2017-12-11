package org.abhijitsarkar.akka.k8s.watcher.persistence

import akka.actor.Props
import com.softwaremill.macwire._
import org.abhijitsarkar.akka.k8s.watcher.ActorModule
import reactivemongo.api.collections.bson.BSONCollection

import scala.concurrent.Future

/**
  * @author Abhijit Sarkar
  */
trait RepositoryModule {
  def createRepositoryActor() = actorModule.actorSystem
    .actorOf(Props(wire[MongoRepositoryActor]))

  def actorModule: ActorModule

  def eventCollection: Future[BSONCollection]
}
