package org.abhijitsarkar.akka.k8s.watcher.domain

import org.abhijitsarkar.akka.k8s.watcher.domain.EventJsonProtocol._
import org.scalatest.{FlatSpec, Matchers}
import spray.json._

/**
  * @author Abhijit Sarkar
  */

class EventJsonProtocolSpec extends FlatSpec with Matchers {
  "EventJsonProtocol" should "unmarshall an event" in {
    val event = io.Source.fromInputStream(getClass.getResourceAsStream("/event.json"))
      .mkString
      .parseJson
      .convertTo[Event]

    event.`type` shouldBe (EventType.ADDED)
  }
}
