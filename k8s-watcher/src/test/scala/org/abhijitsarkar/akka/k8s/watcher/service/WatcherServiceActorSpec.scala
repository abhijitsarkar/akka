package org.abhijitsarkar.akka.k8s.watcher.service

import java.time.Instant

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.{TestKit, TestProbe}
import com.softwaremill.tagging._
import com.typesafe.config.ConfigFactory
import org.abhijitsarkar.akka.k8s.watcher.client.HttpClient
import org.abhijitsarkar.akka.k8s.watcher.domain.EventJsonProtocol._
import org.abhijitsarkar.akka.k8s.watcher.domain._
import org.abhijitsarkar.akka.k8s.watcher.persistence.Repository
import org.abhijitsarkar.akka.k8s.watcher.{ActorModule, K8SProperties}
import org.scalatest._
import spray.json._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * @author Abhijit Sarkar
  */
class WatcherServiceActorSpec extends TestKit(ActorSystem("test", ConfigFactory.load("application-test.conf")
  .withFallback(ConfigFactory.load())))
  with FlatSpecLike
  with Matchers
  with BeforeAndAfterAll
  with EitherValues
  with ServiceModule {
  outer =>

  override val actorModule = new ActorModule {
    override val actorSystem: ActorSystem = outer.system

    override implicit val materializer: Materializer = ActorMaterializer()

    override implicit val executor: ExecutionContext = actorSystem.dispatcher
  }

  override val k8SProperties: K8SProperties = K8SProperties(apiToken = Some("test"), apps = List("test"))
  val httpClient = TestProbe()
  val repository = TestProbe()
  val httpClientActor = httpClient.ref
  val repositoryActor = repository.ref
  val event: Event = io.Source.fromInputStream(getClass.getResourceAsStream("/event.json"))
    .mkString
    .parseJson
    .convertTo[Event]

  lazy val watcherServiceActor = createWatcherServiceActor(
    httpClientActor.taggedWith[HttpClient],
    repositoryActor.taggedWith[Repository]
  )

  override def afterAll(): Unit = {
    shutdown(system)
  }

  "WatcherServiceActor" should "start watching" in {
    watcherServiceActor ! StartWatchingRequest

    val timeout = 3.seconds

    val apps: List[String] = httpClient.receiveOne(timeout) match {
      case GetEventsRequest(apps, _) => apps
    }

    apps should contain("test")
  }

  it should "persist ready events" in {
    watcherServiceActor ! GetEventsResponse(Right(event))

    val timeout = 3.seconds

    val b: Event = repository.receiveOne(timeout) match {
      case PersistEventRequest(a, _) => a
    }

    b.`type` shouldBe (EventType.ADDED)
  }

  it should "not persist not ready events" in {
    val podStatus = event.`object`.status.copy(conditions = List(PodCondition(
      PodConditionStatusType.Initialized,
      PodConditionStatus.True,
      None,
      Instant.now()
    )))

    val pod: Pod = event.`object`.copy(status = podStatus)

    val event2: Event = event.copy(`object` = pod)

    watcherServiceActor ! GetEventsResponse(Right(event2))

    val timeout = 3.seconds

    repository.expectNoMessage(timeout)
  }
}
