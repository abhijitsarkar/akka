package org.abhijitsarkar.akka.k8s.watcher.web

import java.time.temporal.ChronoUnit

import akka.actor.ActorRef
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.testkit.{TestProbe, _}
import com.softwaremill.tagging._
import com.typesafe.config.ConfigFactory
import org.abhijitsarkar.akka.k8s.watcher.domain.{GetAppsRequest, GetStatsForOneRequest, GetStatsRequest}
import org.abhijitsarkar.akka.k8s.watcher.model.Stats
import org.abhijitsarkar.akka.k8s.watcher.model.StatsJsonProtocol._
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration._

/**
  * @author Abhijit Sarkar
  */
class RoutesSpec extends FlatSpecLike
  with Matchers
  with ScalatestRouteTest
  with BeforeAndAfterAll
  with Routes {

  override def testConfig = ConfigFactory.load("application-test.conf")
    .withFallback(ConfigFactory.load())

  val testProbe = TestProbe()
  val testProbeActor = testProbe.ref
    .taggedWith[Web]

  val handler = route(testProbeActor)
  val timeout = 30.seconds
  implicit val routeTestTimout = RouteTestTimeout(timeout.dilated)

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system, 3.seconds, false)
  }

  val app = "test"

  // Putting testProbe inside check hangs forever probably because it creates a deadlock; test waits for callback
  // to be called for the body to execute, and callback in't called until the body is executed.
  "Routes" should "respond to get apps request" in {
    testProbe.setAutoPilot((_: ActorRef, msg: Any) => {
      msg match {
        case GetAppsRequest(callback) => callback(List(app)); TestActor.KeepRunning
        case _ => TestActor.NoAutoPilot
      }
    })

    Get("/apps") ~> handler ~> check {
      entityAs[List[String]] should contain(app)
    }
  }

  it should "respond to get stats request for all apps" in {
    testProbe.setAutoPilot((_: ActorRef, msg: Any) => {
      msg match {
        case GetStatsRequest(callback) => callback(List(Stats(app, ChronoUnit.SECONDS, Nil))); TestActor.KeepRunning
        case _ => TestActor.NoAutoPilot
      }
    })

    Get("/apps/stats") ~> handler ~> check {
      entityAs[List[Stats]].map(_.app) should contain(app)
    }
  }

  it should "respond to get stats request for one app" in {
    testProbe.setAutoPilot((_: ActorRef, msg: Any) => {
      msg match {
        case GetStatsForOneRequest(app, callback) => callback(Stats(app, ChronoUnit.SECONDS, Nil)); TestActor.KeepRunning
        case _ => TestActor.NoAutoPilot
      }
    })
    Get(s"/apps/$app/stats") ~> handler ~> check {
      entityAs[Stats].app shouldBe (app)
    }
  }
}
