package org.abhijitsarkar.akka.k8s.watcher.domain

import org.abhijitsarkar.akka.k8s.watcher.domain.DomainObjectsJsonProtocol._
import org.scalatest.{FlatSpec, Matchers}
import spray.json._

/**
  * @author Abhijit Sarkar
  */

class DomainObjectsJsonProtocolSpec extends FlatSpec with Matchers {
  "WatcherAppJsonProtocol" should "unmarshall an event" in {
    val event = io.Source.fromInputStream(getClass.getResourceAsStream("/event.json"))
      .mkString
      .parseJson
      .convertTo[Event]

    event.`type` shouldBe (EventType.ADDED)
  }

  it should "unmarshall a status" in {
    val status = io.Source.fromInputStream(getClass.getResourceAsStream("/status.json"))
      .mkString
      .parseJson
      .convertTo[Status]

    status.code shouldBe 403
    status.reason shouldBe Some("Forbidden")
    status.message shouldBe ("""pods is forbidden: User "system:serviceaccount:default:default" cannot  pods in the namespace "default"""")
  }
}
