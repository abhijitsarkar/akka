package org.abhijitsarkar.akka.k8s.watcher.persistence

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.{TestKit, TestProbe}
import com.softwaremill.quicklens._
import com.typesafe.config.ConfigFactory
import de.flapdoodle.embed.process.runtime.Network
import org.abhijitsarkar.akka.k8s.watcher.ActorModule
import org.abhijitsarkar.akka.k8s.watcher.domain.DomainObjectsJsonProtocol._
import org.abhijitsarkar.akka.k8s.watcher.domain._
import org.scalatest._
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONDocument
import spray.json._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * @author Abhijit Sarkar
  */
class MongoRepositoryActorSpec extends TestKit(ActorSystem("test", ConfigFactory.load("application-test.conf")
  .withFallback(ConfigFactory.load())))
  with fixture.FlatSpecLike
  with Matchers
  with BeforeAndAfterAll
  with EitherValues
  with RepositoryModule {
  outer =>
  override val actorModule = new ActorModule {
    override val actorSystem: ActorSystem = outer.system

    override implicit val materializer: Materializer = ActorMaterializer()

    override implicit val executor: ExecutionContext = actorSystem.dispatcher
  }

  import actorModule._

  lazy val mongoRepositoryActor = createRepositoryActor()
  val event: Event = io.Source.fromInputStream(getClass.getResourceAsStream("/event.json"))
    .mkString
    .parseJson
    .convertTo[Event]
  val testProbe = TestProbe()
  val testProbeActor = testProbe.ref
  var eventCollection: Future[BSONCollection] = _
  val mongoProperties = MongoProperties(uri = s"mongodb://localhost:${Network.getFreeServerPort()}/test")

  override def beforeAll() {
    EmbeddedMongoServer.start(mongoProperties.host, mongoProperties.port)

    eventCollection = MongoCollectionFactory.collection(mongoProperties)
  }

  override def afterAll(): Unit = {
    EmbeddedMongoServer.stop(mongoProperties.host, mongoProperties.port)
    shutdown(system)
  }

  def withFixture(test: OneArgTest) = {
    mongoRepositoryActor ! PersistEventRequest(event, testProbeActor)

    val timeout = 5.seconds

    val x = testProbe.receiveOne(timeout) match {
      case PersistEventResponse((_, Right(n))) => n
      case unknown => fail(s"Unknown message: $unknown.")
    }

    val theFixture = FixtureParam(x)

    try {
      withFixture(test.toNoArgTest(theFixture)) // "loan" the fixture to the test
    }
    finally {
      val x = for {
        coll <- eventCollection
        result <- coll.remove(BSONDocument())
      } yield result

      Await.result(x, 3.seconds)
    }
  }

  case class FixtureParam(result: Int)

  "MongoRepositoryActor" should "persist events" in { e =>
    e.result shouldBe (1)
  }

  it should "find a persisted event by pod uid" in { e =>
    e.result shouldBe (1)

    mongoRepositoryActor ! FindByPodUidRequest(event.`object`.metadata.uid, testProbeActor)

    val timeout = 3.seconds

    testProbe.receiveOne(timeout) match {
      case FindByPodUidResponse(opt) => {
        opt shouldBe defined
      }
    }
  }

  it should "find a persisted event by app" in { e =>
    e.result shouldBe (1)

    mongoRepositoryActor ! FindByAppRequest(event.`object`.metadata.labels("app"), testProbeActor, "")

    val timeout = 3.seconds

    testProbe.receiveOne(timeout) match {
      case FindByAppResponse(events, _) => {
        events should not be empty
      }
    }
  }

  it should "overwrite event if already present" in { e =>
    e.result shouldBe (1)

    mongoRepositoryActor ! PersistEventRequest(event, testProbeActor)

    val timeout = 3.seconds

    val x = testProbe.receiveOne(timeout) match {
      case PersistEventResponse((_, Right(n))) => n
    }

    x shouldBe (1)

    mongoRepositoryActor ! FindByAppRequest(event.`object`.metadata.labels("app"), testProbeActor, "")

    testProbe.receiveOne(timeout) match {
      case FindByAppResponse(events, _) => {
        events.size shouldBe (1)
      }
    }
  }

  it should "limit find results to 25 documents" in { e =>
    //    val pod: Lens[Event, Pod] = GenLens[Event](_.`object`)
    //    val metadata: Lens[Pod, Metadata] = GenLens[Pod](_.metadata)
    //    val labels: Lens[Metadata, Map[String, String]] = GenLens[Metadata](_.labels)

    (1 to 30)
      .map { _ =>
        // lens FTW
        event.modify(_.`object`.metadata.uid).using(_ => UUID.randomUUID.toString())
        //        event.copy(
        //          `object` = event.`object`.copy(
        //            metadata = event.`object`.metadata.copy(
        //              uid = UUID.randomUUID.toString()
        //            )
        //          )
        //        )
      }
      .foreach(mongoRepositoryActor ! PersistEventRequest(_, testProbeActor))

    mongoRepositoryActor ! FindByAppRequest(event.`object`.metadata.labels("app"), testProbeActor, "")

    val timeout = 5.seconds

    testProbe.receiveN(30, timeout) match {
      case ack: Vector[_] => ack should have length (30)
    }

    testProbe.receiveOne(timeout) match {
      case FindByAppResponse(events, _) => {
        events should have length (25)
      }
    }
  }
}
