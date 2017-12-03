package org.abhijitsarkar.akka.k8s.watcher.web

import java.time.temporal.ChronoUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.stream.Materializer
import akka.testkit.{TestProbe, _}
import com.softwaremill.tagging._
import org.abhijitsarkar.akka.k8s.watcher.{ActorModule, K8SProperties}
import org.abhijitsarkar.akka.k8s.watcher.domain.{GetAppsRequest, GetStatsForOneRequest, GetStatsRequest}
import org.abhijitsarkar.akka.k8s.watcher.model.Stats
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
import org.abhijitsarkar.akka.k8s.watcher.model.StatsJsonProtocol._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

/**
  * @author Abhijit Sarkar
  */
class RoutesSpec extends FlatSpecLike
  with Matchers
  with ScalatestRouteTest
  with WebModule
  with BeforeAndAfterAll
  with Routes {
  outer =>

  override def afterAll(): Unit = {
    Await.result(system.terminate(), 3.seconds)
  }

  override val actorModule = new ActorModule {
    override val actorSystem: ActorSystem = outer.system

    override val materializer: Materializer = outer.materializer

    override val executor: ExecutionContext = outer.executor
  }

  override def k8SProperties = new K8SProperties(apps = List("test"))

  val testProbe = TestProbe()
  val testProbeActor = testProbe.ref
    .taggedWith[Web]

  val timeout = 1.minute

  ignore should "respond to get apps request" in {
    implicit val routeTestTimout = RouteTestTimeout(timeout.dilated)
    Get("/apps") ~> route(testProbeActor) ~> check {

      testProbe.receiveOne(timeout) match {
        case GetAppsRequest(callback) => {
          callback(k8SProperties.apps)
        }
      }
      entityAs[List[String]] should contain("test")
    }
    testProbe.expectNoMessage(timeout)
  }

  ignore should "respond to get stats request for all apps" in {
    implicit val routeTestTimout = RouteTestTimeout(timeout.dilated)
    val app = "test"
    Get("/apps/stats") ~> route(testProbeActor) ~> check {

      testProbe.receiveOne(timeout) match {
        case GetStatsRequest(callback) => {
          callback(List(Stats(app, ChronoUnit.SECONDS, Nil)))
        }
        case other => fail(s"Unexpected message $other.")
      }
      entityAs[List[Stats]].size shouldBe (1)
      entityAs[List[Stats]].head.app shouldBe (app)
    }
    testProbe.expectNoMessage(timeout)
  }

  ignore should "respond to get stats request for one app" in {
    implicit val routeTestTimout = RouteTestTimeout(timeout.dilated)
    val app = "test"
    Get(s"/apps/$app/stats") ~> route(testProbeActor) ~> check {

      testProbe.receiveOne(timeout) match {
        case GetStatsForOneRequest(app, callback) => {
          callback(Stats(app, ChronoUnit.SECONDS, Nil))
        }
        case other => fail(s"Unexpected message $other.")
      }
      entityAs[Stats].app shouldBe (app)
    }
    testProbe.expectNoMessage(timeout)
  }
}
