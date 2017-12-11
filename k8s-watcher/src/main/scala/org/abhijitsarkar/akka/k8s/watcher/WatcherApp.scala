package org.abhijitsarkar.akka.k8s.watcher

import akka.actor.ActorSystem
import akka.http.scaladsl.server.HttpApp
import akka.stream.{ActorMaterializer, Materializer}
import com.softwaremill.tagging._
import com.typesafe.config.{Config, ConfigFactory}
import org.abhijitsarkar.akka.k8s.watcher.client.{ClientModule, HttpClient}
import org.abhijitsarkar.akka.k8s.watcher.domain.StartWatchingRequest
import org.abhijitsarkar.akka.k8s.watcher.persistence._
import org.abhijitsarkar.akka.k8s.watcher.service.ServiceModule
import org.abhijitsarkar.akka.k8s.watcher.web.{Routes, WebModule}
import org.slf4j.LoggerFactory
import pureconfig.ConvertHelpers.catchReadError
import pureconfig.syntax._
import pureconfig.{ConfigReader, loadConfigOrThrow}

import scala.concurrent.ExecutionContext
import scala.util.Try

/**
  * @author Abhijit Sarkar
  */
object WatcherApp extends HttpApp with App with ClientModule with RepositoryModule with ServiceModule with WebModule with Routes {
  val log = LoggerFactory.getLogger(WatcherApp.getClass)
  implicit val smarterReader =
    ConfigReader.fromString[List[String]](catchReadError(_.split(",").map(_.trim).toList))

  val config = ConfigFactory.load.to[Config].right.get

  override val actorModule = new ActorModule {
    override implicit val actorSystem: ActorSystem = ActorSystem("k8s-watcher", config)

    override implicit val materializer: Materializer = ActorMaterializer()

    override implicit val executor: ExecutionContext = actorSystem.dispatcher
  }

  import actorModule._

  val k8SProperties = loadConfigOrThrow[K8SProperties](config, "k8s")

  val mongoProperties = loadConfigOrThrow[MongoProperties](config, "mongo")

  if (mongoProperties.embedded) {
    EmbeddedMongoServer.start(mongoProperties.host, mongoProperties.port)

    implicitly[ActorSystem].registerOnTermination(() => EmbeddedMongoServer.stop(mongoProperties.host, mongoProperties.port))
  }

  implicitly[ActorSystem].registerOnTermination(() => MongoCollectionFactory.closeDriver())

  override val eventCollection = MongoCollectionFactory.collection(mongoProperties)
//    .andThen {
//      case Success(coll) => {
//        val create: Future[Unit] = coll.db.collection[BSONCollection]("akka_persistence_metadata").create().recover {
//          case t if (t.getMessage.contains("already exists")) =>
//          case t => log.error("Failed to create Akka Persistence collection.", t)
//        }
//        Await.result(create, 3.seconds)
//      }
//    }

  lazy val repositoryActor = createRepositoryActor()
    .taggedWith[Repository]
  lazy val clientActor = createClientActor()
    .taggedWith[HttpClient]

  lazy val serviceActor = createWatcherServiceActor(clientActor, repositoryActor)

  serviceActor ! StartWatchingRequest

  lazy val requestHandlerActor = createRequestHandlerActor(repositoryActor)

  val port = Try(config.getInt("k8s-watcher.port")).getOrElse(8080)

  override protected def routes = route(requestHandlerActor)

  WatcherApp.startServer("localhost", port, actorSystem)
}
