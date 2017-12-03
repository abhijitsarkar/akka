package org.abhijitsarkar.akka.k8s.watcher.client

import akka.actor.Props
import com.softwaremill.macwire._
import org.abhijitsarkar.akka.k8s.watcher.{ActorModule, K8SProperties}

/**
  * @author Abhijit Sarkar
  */
trait ClientModule {
  def createClientActor() = actorModule.actorSystem
    .actorOf(Props(wire[K8SClientActor]))

  def actorModule: ActorModule

  def k8SProperties: K8SProperties
}
