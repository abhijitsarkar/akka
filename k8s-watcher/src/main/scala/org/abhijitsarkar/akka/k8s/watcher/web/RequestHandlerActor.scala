package org.abhijitsarkar.akka.k8s.watcher.web

import java.time.temporal.ChronoUnit.SECONDS
import java.util.UUID
import java.util.concurrent.{ConcurrentHashMap => JavaConcurrentMap}

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.softwaremill.tagging.@@
import org.abhijitsarkar.akka.k8s.watcher.{ActorModule, K8SProperties}
import org.abhijitsarkar.akka.k8s.watcher.domain._
import org.abhijitsarkar.akka.k8s.watcher.model.Stats
import org.abhijitsarkar.akka.k8s.watcher.repository.Repository

import scala.collection.JavaConverters._
import scala.collection.concurrent.{Map => ConcurrentMap}

/**
  * @author Abhijit Sarkar
  */
trait Web

class RequestHandlerActor(
                           k8SProperties: K8SProperties,
                           repositoryActor: ActorRef @@ Repository,
                           actorModule: ActorModule
                         ) extends Actor with ActorLogging {
  type ChunkedResponse = (Int, List[(String, List[Event])], List[Stats] => Unit)
  type SimpleResponse = (String, Stats => Unit)
  private val responseMap: ConcurrentMap[UUID, Either[ChunkedResponse, SimpleResponse]] =
    (new JavaConcurrentMap[UUID, Either[ChunkedResponse, SimpleResponse]]()).asScala

  override def receive = {
    case GetStatsForOneRequest(app, callback) => {
      val requestId = UUID.randomUUID()
      responseMap.put(requestId, Right((app, callback)))
      repositoryActor ! FindByAppRequest(app, self, requestId)
    }
    case GetStatsRequest(callback) => {
      val requestId = UUID.randomUUID()
      responseMap.put(requestId, Left((k8SProperties.apps.size, Nil, callback)))

      k8SProperties.apps.foreach(app => repositoryActor ! FindByAppRequest(app, self, requestId))
    }
    case FindByAppResponse(events, requestId) => {
      val updatedList = (list: List[(String, List[Event])], events: List[Event]) => {
        if (events.isEmpty) {
          list
        } else {
          val app = events
            .find(_.`object`.app.isDefined)
            .map(_.`object`.app.get)
            .getOrElse("UNKNOWN")
          (app, events) :: list
        }
      }

      val startupDurations = (events: List[Event]) => events.map(_.`object`.startupDuration(SECONDS))

      responseMap(requestId) match {
        case Left((1, list, callback)) => {
          responseMap.remove(requestId)
          callback(updatedList(list, events).map(x => Stats(x._1, SECONDS, startupDurations(x._2))))
        }
        case Left((count, list, callback)) => {
          val updated = updatedList(list, events)
          responseMap.update(requestId, Left((count - 1, updated, callback)))
        }
        case Right((app, callback)) => callback(Stats(app, SECONDS, startupDurations(events)))
      }
    }
    case GetAppsRequest(callback) => {
      callback(k8SProperties.apps)
    }
  }
}
