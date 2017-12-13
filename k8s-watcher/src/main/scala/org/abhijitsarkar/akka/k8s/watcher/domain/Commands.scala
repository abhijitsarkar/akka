package org.abhijitsarkar.akka.k8s.watcher.domain

import akka.actor.ActorRef
import org.abhijitsarkar.akka.k8s.watcher.model.Stats

/**
  * @author Abhijit Sarkar
  */
// K8S
case class GetEventsRequest(apps: List[String], replyTo: ActorRef)

case class GetEventsResponse(event: Either[String, Event])

case class DeletePodsRequest(apps: List[String], replyTo: ActorRef)

case class DeletePodsResponse(response: Option[Status])

// Mongo

case class PersistEventRequest(event: Event, replyTo: ActorRef)

case class PersistEventResponse(result: (String, Either[String, Int]))

case class FindByPodUidRequest(uid: String, replyTo: ActorRef)

case class FindByPodUidResponse(event: Option[Event])

case class FindByAppRequest(app: String, replyTo: ActorRef, uuid: String)

case class FindByAppResponse(events: List[Event], uuid: String)

case object StartWatchingRequest

case class GetStatsForOneRequest(app: String, callback: Stats => Unit)

case class GetStatsRequest(callback: List[Stats] => Unit)

case class GetAppsRequest(callback: List[String] => Unit)

