package org.abhijitsarkar.akka.k8s.watcher.service

import akka.actor.{ActorRef, Props}
import com.softwaremill.macwire.wire
import com.softwaremill.tagging.{@@, _}
import org.abhijitsarkar.akka.k8s.watcher.{ActorModule, K8SProperties}
import org.abhijitsarkar.akka.k8s.watcher.client.HttpClient
import org.abhijitsarkar.akka.k8s.watcher.repository.Repository

/**
  * @author Abhijit Sarkar
  */
trait ServiceModule {

  def createWatcherServiceActor(httpClientActor: ActorRef @@ HttpClient,
                                repositoryActor: ActorRef @@ Repository) =
    actorModule.actorSystem.actorOf(Props(wire[WatcherServiceActor]))
      .taggedWith[Service]

  def actorModule: ActorModule

  def k8SProperties: K8SProperties
}
