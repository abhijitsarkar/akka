package org.abhijitsarkar.akka.k8s.watcher.service

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.softwaremill.tagging.@@
import org.abhijitsarkar.akka.k8s.watcher.{ActorModule, K8SProperties}
import org.abhijitsarkar.akka.k8s.watcher.client.HttpClient
import org.abhijitsarkar.akka.k8s.watcher.domain._
import org.abhijitsarkar.akka.k8s.watcher.persistence.Repository

/**
  * @author Abhijit Sarkar
  */
class WatcherServiceActor(
                           httpClientActor: ActorRef @@ HttpClient,
                           repositoryActor: ActorRef @@ Repository,
                           k8SProperties: K8SProperties,
                           actorModule: ActorModule
                         ) extends Actor with ActorLogging {

  override def receive = {
    case StartWatchingRequest => {
      httpClientActor ! GetEventsRequest(k8SProperties.apps, self)
    }
    case GetEventsResponse(event) => {
      (event match {
        case Right(e) => log.debug("Received event: {}.", e); Some(e).filter(_.`object`.ready)
        case Left(error) => log.error("Failed to retrieve events. Cause: {}.", error); None
      })
        .foreach(repositoryActor ! PersistEventRequest(_, self))
    }

    case PersistEventResponse(result)
    => {
      result match {
        case (app, Right(n)) => log.debug("Successfully persisted {} event(s) for app: {}.", n, app)
        case (app, Left(err)) => log.error("Failed to persist event for app: {}. Cause: {}.", app, err)
      }
    }
  }
}

trait Service
