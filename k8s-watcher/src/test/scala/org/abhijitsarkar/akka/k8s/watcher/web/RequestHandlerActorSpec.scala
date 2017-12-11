package org.abhijitsarkar.akka.k8s.watcher.web

import java.time.temporal.ChronoUnit

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.{TestKit, TestProbe}
import com.softwaremill.tagging._
import com.typesafe.config.ConfigFactory
import org.abhijitsarkar.akka.k8s.watcher.domain.EventJsonProtocol._
import org.abhijitsarkar.akka.k8s.watcher.domain._
import org.abhijitsarkar.akka.k8s.watcher.model.Stats
import org.abhijitsarkar.akka.k8s.watcher.persistence.Repository
import org.abhijitsarkar.akka.k8s.watcher.{ActorModule, K8SProperties, domain}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
import spray.json._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * @author Abhijit Sarkar
  */
class RequestHandlerActorSpec extends TestKit(ActorSystem("test", ConfigFactory.load("application-test.conf")))
  with FlatSpecLike
  with Matchers
  with BeforeAndAfterAll
  with WebModule {
  outer =>

  override val actorModule = new ActorModule {
    override val actorSystem: ActorSystem = outer.system

    override implicit val materializer: Materializer = ActorMaterializer()

    override implicit val executor: ExecutionContext = actorSystem.dispatcher
  }

  val event = io.Source.fromInputStream(getClass.getResourceAsStream("/event.json"))
    .mkString
    .parseJson
    .convertTo[Event]

  override val k8SProperties: K8SProperties = K8SProperties(apps = List("test1", "test2"))

  val testProbe = TestProbe()
  val testProbeActor = testProbe.ref
    .taggedWith[Repository]

  lazy val requestHandlerActor = createRequestHandlerActor(testProbeActor)
  val timeout = 3.seconds

  "RequestHandlerActor" should "get apps" in {
    requestHandlerActor ! GetAppsRequest(apps => {
      apps should contain theSameElementsAs k8SProperties.apps
    })
  }

  "RequestHandlerActor" should "get stats for one app" in {
    val callback: Stats => Unit = (stats: Stats) => stats match {
      case Stats(app, unit, events) => {
        app shouldBe "test"
        unit shouldBe (ChronoUnit.SECONDS)
        events should have length 1
      }
    }

    requestHandlerActor ! GetStatsForOneRequest("test", callback)
    testProbe.receiveOne(timeout) match {
      case FindByAppRequest(app, _, uuid) => {
        app shouldBe ("test")
        testProbe.send(requestHandlerActor, FindByAppResponse(List(event), uuid))
      }
    }
    testProbe.expectNoMessage(timeout)
  }

  "RequestHandlerActor" should "get stats for all apps" in {
    val callback: List[Stats] => Unit = (stats: List[Stats]) => stats match {
      case Seq(Stats(app1, unit1, events1), Stats(app2, unit2, events2)) => {
        app1 shouldBe "test"
        events1 should have length 1
        app1 shouldBe "test"
        events2 should have length 1
      }
    }

    requestHandlerActor ! GetStatsRequest(callback)

    testProbe.receiveN(2, timeout) match {
      case Seq(FindByAppRequest(app1, _, uuid1), FindByAppRequest(app2, _, uuid2)) => List(app1, app2)
        .foreach(app => app should (equal("test1") or equal("test2")))
        testProbe.send(requestHandlerActor, domain.FindByAppResponse(List(event), uuid2))
        testProbe.send(requestHandlerActor, domain.FindByAppResponse(List(event), uuid1))
    }
    testProbe.expectNoMessage(timeout)
  }
}
