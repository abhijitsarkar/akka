package org.abhijitsarkar.akka.k8s.watcher.domain

import java.util.UUID

import akka.actor.ActorRef
import org.abhijitsarkar.akka.k8s.watcher.model.Stats

/**
  * @author Abhijit Sarkar
  */
case class GetEventsRequest(apps: List[String], replyTo: ActorRef)

case class GetEventsResponse(event: Either[String, Event])

case class PersistEventRequest(event: Event, replyTo: ActorRef)

case class PersistEventResponse(result: (String, Either[String, Int]))

case class FindByPodUidRequest(uid: String, replyTo: ActorRef)

case class FindByPodUidResponse(event: Option[Event])

case class FindByAppRequest(app: String, replyTo: ActorRef, uuid: UUID)

case class FindByAppResponse(events: List[Event], uuid: UUID)

case object StartWatchingRequest

case class GetStatsForOneRequest(app: String, callback: Stats => Unit)

case class GetStatsRequest(callback: List[Stats] => Unit)

case class GetAppsRequest(callback: List[String] => Unit)
