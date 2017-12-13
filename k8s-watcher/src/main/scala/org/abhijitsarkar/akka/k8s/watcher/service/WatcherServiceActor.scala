package org.abhijitsarkar.akka.k8s.watcher.service

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.softwaremill.tagging.@@
import org.abhijitsarkar.akka.k8s.watcher.client.HttpClient
import org.abhijitsarkar.akka.k8s.watcher.domain._
import org.abhijitsarkar.akka.k8s.watcher.persistence.Repository
import org.abhijitsarkar.akka.k8s.watcher.{ActorModule, K8SProperties}

import scala.concurrent.duration._

/**
  * @author Abhijit Sarkar
  */
class WatcherServiceActor(
                           httpClientActor: ActorRef @@ HttpClient,
                           repositoryActor: ActorRef @@ Repository,
                           k8SProperties: K8SProperties,
                           actorModule: ActorModule
                         ) extends Actor with ActorLogging {

  override def preStart(): Unit = {
    super.preStart()

    import actorModule.executor
    if (k8SProperties.deletionEnabled) {
      log.info("Scheduling deletion of apps beginning after: {} min and thereafter every: {} min.",
        k8SProperties.deletionInitialDelayMin, k8SProperties.deletionIntervalMin
      )
      context.system.scheduler.schedule(
        k8SProperties.deletionInitialDelayMin.minutes,
        k8SProperties.deletionIntervalMin.minutes,
        httpClientActor,
        DeletePodsRequest(k8SProperties.apps, self))
    }
  }

  override def receive = {
    case StartWatchingRequest => {
      httpClientActor ! GetEventsRequest(k8SProperties.apps, self)
    }
    case GetEventsResponse(event) => {
      (event match {
        case Right(e) => log.debug("Received event: {}.", e); Some(e).filter(_.`object`.ready)
        case Left(error) => log.error("Failed to retrieve events. Reason: {}.", error); None
      })
        .foreach(repositoryActor ! PersistEventRequest(_, self))
    }
    case PersistEventResponse(result) => {
      result match {
        case (app, Right(n)) => log.debug("Successfully persisted {} event(s) for app: {}.", n, app)
        case (app, Left(err)) => log.error("Failed to persist event for app: {}. Reason: {}.", app, err)
      }
    }
    case DeletePodsResponse(result) => {
      result match {
        case Some(status) => log.error("Failed to delete pod(s). Status: {}.", status)
        case _ => log.info("Successfully deleted pod(s).")
      }
    }
  }
}

trait Service
