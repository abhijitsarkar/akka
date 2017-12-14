package org.abhijitsarkar.akka.k8s.watcher.client

import akka.actor.ActorSystem
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.headers._
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.{TestKit, TestProbe}
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.common.Slf4jNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.typesafe.config.ConfigFactory
import org.abhijitsarkar.akka.k8s.watcher.domain._
import org.abhijitsarkar.akka.k8s.watcher.{ActorModule, K8SProperties}
import org.scalatest._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * @author Abhijit Sarkar
  */
class K8SClientActorSpec extends TestKit(ActorSystem("test", ConfigFactory.load("application-test.conf")
  .withFallback(ConfigFactory.load())))
  with FlatSpecLike
  with Matchers
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with EitherValues
  with ClientModule {
  outer =>

  override val actorModule = new ActorModule {
    override val actorSystem: ActorSystem = outer.system

    override implicit val materializer: Materializer = ActorMaterializer()

    override implicit val executor: ExecutionContext = actorSystem.dispatcher
  }

  override val k8SProperties: K8SProperties = K8SProperties(apiToken = Some("test"))
  val wireMockServer = new WireMockServer(wireMockConfig().port(9000).notifier(new Slf4jNotifier(true)))

  override def beforeAll() {
    wireMockServer.start()
  }

  override def afterAll(): Unit = {
    wireMockServer.stop()
    shutdown(system)
  }

  override def afterEach(): Unit = {
    wireMockServer.resetAll()
  }

  "K8SClientActor" should "fetch events" in {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/v1/watch/namespaces/default/pods"))
      .withHeader(Accept.name, equalTo(`application/json`.value))
      .withHeader(Authorization.name, equalTo("Bearer test"))
      .withQueryParam("labelSelector", equalTo("app=test"))
      .withQueryParam("includeUninitialized", equalTo("false"))
      .withQueryParam("pretty", equalTo("false"))
      .withQueryParam("export", equalTo("true"))
      .willReturn(okJson((io.Source.fromInputStream(getClass.getResourceAsStream("/event.json")).mkString)))
    )

    val testProbe = TestProbe()
    createClientActor() ! GetEventsRequest(List("test"), testProbe.ref)

    val timeout = 3.seconds

    testProbe.receiveOne(timeout) match {
      case GetEventsResponse(result) => result.right.value.`type` shouldBe (EventType.ADDED)
    }
  }

  it should "delete pods" in {
    wireMockServer.stubFor(delete(urlPathEqualTo("/api/v1/namespaces/default/pods"))
      .withHeader(Accept.name, equalTo(`application/json`.value))
      .withHeader(Authorization.name, equalTo("Bearer test"))
      .withQueryParam("labelSelector", equalTo("app=test"))
      .withQueryParam("pretty", equalTo("false"))
      .withQueryParam("includeUninitialized", equalTo("false"))
      .willReturn(ok("\n")))

    val testProbe = TestProbe()
    createClientActor() ! DeletePodsRequest(List("test"), testProbe.ref)

    val timeout = 5.seconds

    testProbe.receiveOne(timeout) match {
      case DeletePodsResponse(result) => result shouldBe None
    }
  }
}
