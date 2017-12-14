package org.abhijitsarkar.akka.k8s.watcher.client

import akka.NotUsed
import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.sprayJsonByteStringUnmarshaller
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.model.headers.{Accept, Authorization, OAuth2BearerToken}
import akka.http.scaladsl.model.{HttpRequest, _}
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.http.scaladsl.unmarshalling.{FromByteStringUnmarshaller, Unmarshal, Unmarshaller}
import akka.stream.scaladsl.{Flow, Framing, MergeHub, Sink, Source}
import akka.util.ByteString
import org.abhijitsarkar.akka.k8s.watcher.domain.DomainObjectsJsonProtocol._
import org.abhijitsarkar.akka.k8s.watcher.domain._
import org.abhijitsarkar.akka.k8s.watcher.{ActorModule, K8SProperties}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

/**
  * @author Abhijit Sarkar
  */
trait HttpClient

class K8SClientActor(
                      k8SProperties: K8SProperties,
                      actorModule: ActorModule
                    ) extends Actor with ActorLogging {

  import actorModule._

  private val authToken: String = {
    k8SProperties.apiToken
      .orElse(k8SProperties.apiTokenFile
        .filterNot(_.isEmpty)
        .map(x => io.Source.fromFile(x).mkString))
      .getOrElse("")
  }

  val timeout = 10.seconds
  if (authToken.isEmpty) {
    log.error("One of apiToken and apiTokenFile must be defined. Stopping system.")
    Await.result(actorSystem.terminate(), timeout)
  }

  val origSettings = ConnectionPoolSettings(context.system.settings.config)
  val newSettings = origSettings //.withIdleTimeout(origSettings.idleTimeout)

  private type RequestPair = (HttpRequest, NotUsed)
  private type ResponsePair = (Try[HttpResponse], NotUsed)
  // super pool encrypts the connection by looking at the protocol
  private lazy val poolFlow: Flow[RequestPair, ResponsePair, _] = Http().superPool[NotUsed](settings = newSettings)

  private def responseValidationFlow[L, R](responsePair: ResponsePair)(
    implicit ev1: FromByteStringUnmarshaller[L], ev2: FromByteStringUnmarshaller[R], rTag: ClassTag[R]
  ): Source[Either[L, R], Any] = {
    responsePair match {
      case (Success(response), _) => {
        response.entity.dataBytes
          .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 8192))
          .mapAsyncUnordered(Runtime.getRuntime.availableProcessors()) { body =>
            if (response.status == OK) {
              val obj: Future[R] = Unmarshal(body).to[R]
              obj.foreach(_ => log.debug("Received {}.", rTag.runtimeClass.getSimpleName))
              obj.map(Right(_))
            } else {
              log.error("Non 200 response status: {}.", response.status.intValue())
              Unmarshal(body).to[L]
                .map(Left(_))
            }
          }
      }
      case (Failure(t), _) => {
        log.error(t, "Request failed.")
        Source.empty
      }
    }
  }

  // https://github.com/akka/akka-http/issues/63
  override def receive = {
    case GetEventsRequest(apps, replyTo) => {
      val request: String => HttpRequest = (app: String) => RequestBuilding.Get(Uri(k8SProperties.baseUrl)
        .withPath(Path(s"/api/v1/watch/namespaces/${k8SProperties.namespace}/pods"))
        .withQuery(Query(Map(
          "labelSelector" -> s"app=$app",
          "export" -> "true",
          "includeUninitialized" -> "false",
          "pretty" -> "false"
        )))
      )
        .withHeaders(
          Accept(MediaRange(MediaTypes.`application/json`)),
          Authorization(OAuth2BearerToken(authToken))
        )

      // https://doc.akka.io/docs/akka/current/stream/stream-dynamic.html
      val sink = Sink.foreach[Either[String, Event]](replyTo ! GetEventsResponse(_))
      val merge = MergeHub.source[Either[String, Event]](perProducerBufferSize = 16).to(sink)
      val consumer = merge.run()

      implicit val stringUnmarshaller: FromByteStringUnmarshaller[String] =
        Unmarshaller.strict(_.utf8String)

      Source(apps)
        .map(app => (request(app), NotUsed))
        .via(poolFlow)
        .flatMapMerge(16, responseValidationFlow[String, Event])
        .runWith(consumer)
    }

    case DeletePodsRequest(apps, replyTo) => {
      val request: String => HttpRequest = (app: String) => RequestBuilding.Delete(Uri(k8SProperties.baseUrl)
        .withPath(Path(s"/api/v1/namespaces/${k8SProperties.namespace}/pods"))
        .withQuery(Query(Map(
          "labelSelector" -> s"app=$app",
          "includeUninitialized" -> "false",
          "pretty" -> "false"
        )))
      )
        .withHeaders(
          Accept(MediaRange(MediaTypes.`application/json`)),
          Authorization(OAuth2BearerToken(authToken))
        )

      // https://doc.akka.io/docs/akka/current/stream/stream-dynamic.html
      val sink = Sink.foreach[Option[Status]](replyTo ! DeletePodsResponse(_))
      val merge = MergeHub.source[Option[Status]](perProducerBufferSize = 16).to(sink)
      val consumer = merge.run()

      implicit val unitUnmarshaller: FromByteStringUnmarshaller[Unit] =
        Unmarshaller.strict(_.utf8String)

      Source(apps)
        .map(app => (request(app), NotUsed))
        .via(poolFlow)
        .flatMapMerge(16, responseValidationFlow[Status, Unit])
        .map {
          _ match {
            case Left(status) => Some(status)
            case _ => None
          }
        }
        .runWith(consumer)
    }
  }
}
