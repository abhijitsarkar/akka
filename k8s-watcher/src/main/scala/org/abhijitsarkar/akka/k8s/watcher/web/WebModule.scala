package org.abhijitsarkar.akka.k8s.watcher.web

import akka.actor.{ActorRef, Props}
import com.softwaremill.macwire.wire
import com.softwaremill.tagging.{@@, _}
import org.abhijitsarkar.akka.k8s.watcher.persistence.Repository
import org.abhijitsarkar.akka.k8s.watcher.{ActorModule, K8SProperties}

/**
  * @author Abhijit Sarkar
  */
trait WebModule {
  def createRequestHandlerActor(repositoryActor: ActorRef @@ Repository) =
    actorModule.actorSystem.actorOf(Props(wire[RequestHandlerActor]))
      .taggedWith[Web]

  def actorModule: ActorModule

  def k8SProperties: K8SProperties
}
