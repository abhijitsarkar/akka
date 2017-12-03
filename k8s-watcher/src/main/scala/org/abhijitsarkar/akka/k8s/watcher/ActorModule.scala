package org.abhijitsarkar.akka.k8s.watcher

import akka.actor.ActorSystem
import akka.event.LogSource
import akka.stream.Materializer

import scala.concurrent.ExecutionContext

/**
  * @author Abhijit Sarkar
  */
trait ActorModule {
  implicit def actorSystem: ActorSystem

  implicit def materializer: Materializer

  implicit def executor: ExecutionContext

  implicit val logSource: LogSource[AnyRef] = new LogSource[AnyRef] {
    def genString(o: AnyRef): String = o.getClass.getName

    override def getClazz(o: AnyRef): Class[_] = o.getClass
  }
}
