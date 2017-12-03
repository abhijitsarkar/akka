package org.abhijitsarkar.akka.k8s.watcher.client

import akka.NotUsed
import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.model.headers.{Accept, Authorization, OAuth2BearerToken}
import akka.http.scaladsl.model.{HttpRequest, _}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorAttributes
import akka.stream.scaladsl.{Flow, Framing, Source}
import akka.util.ByteString
import org.abhijitsarkar.akka.k8s.watcher.domain.EventJsonProtocol._
import org.abhijitsarkar.akka.k8s.watcher.{ActorModule, K8SProperties}
import org.abhijitsarkar.akka.k8s.watcher.domain.{Event, GetEventsRequest, GetEventsResponse}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
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

  val timeout = 10.seconds

  if (k8SProperties.baseUrl.startsWith("https") && k8SProperties.certFile.isEmpty) {
    log.error("Endpoint is secured, but certFile isn't defined. Stopping system.")
    Await.result(actorSystem.terminate(), timeout)
  }

  private val authToken: String = {
    k8SProperties.apiToken
      .orElse(k8SProperties.apiTokenFile
        .filterNot(_.isEmpty)
        .map(x => io.Source.fromFile(x).mkString))
      .getOrElse("")
  }

  if (authToken.isEmpty) {
    log.error("One of apiToken and apiTokenFile must be defined. Stopping system.")
    Await.result(actorSystem.terminate(), timeout)
  }

  private type RequestPair = (HttpRequest, NotUsed)

  private type ResponsePair = (Try[HttpResponse], NotUsed)
  private lazy val poolFlow: Flow[RequestPair, ResponsePair, _] =
    Http().cachedHostConnectionPool[NotUsed](
      host = k8SProperties.host,
      port = k8SProperties.port
    )

  //  private lazy val connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
  //    Http().outgoingConnection(host = k8SProperties.host, port = k8SProperties.port)
  //
  //  private val responseValidationFlow = (response: HttpResponse) => {
  //    response.entity.dataBytes
  //      .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 8096))
  //      .mapAsyncUnordered(Runtime.getRuntime.availableProcessors()) { data =>
  //        if (response.status == OK) {
  //          Unmarshal(data).to[Event]
  //            .map(Right(_))
  //        } else {
  //          Future.successful(data.utf8String)
  //            .map(Left(_))
  //        }
  //      }
  //  }

  private val responseValidationFlow = (responsePair: ResponsePair) => responsePair match {
    case (Success(response), _) => {
      response.entity.dataBytes
        .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 8096))
        .mapAsyncUnordered(Runtime.getRuntime.availableProcessors()) { data =>
          if (response.status == OK) {
            val event: Future[Event] = Unmarshal(data).to[Event]
            event.foreach(x => log.debug("Received event: {}.", x))
            event.map(Right(_))
          } else {
            Future.successful(data.utf8String)
              .map(Left(_))
          }
        }
      //        .map(EitherT.fromEither[Future](_))

      //          val e: Future[Either[String, Event]] = response.entity.toStrict(timeout)
      //            .flatMap { strict =>
      //              if (response.status == OK) {
      //                val event = Unmarshal(strict).to[Event]
      //                event.foreach(x => log.debug("Received event: {}.", x))
      //                event.map(Right(_))
      //              } else {
      //                Unmarshal(strict).to[String].map(Left(_))
      //              }
      //            }
    }
    case (Failure(t), _) => {
      Source.single(Left(t.getMessage))
    }
  }

  val dispatchers = context.system.dispatchers
  val blockingDispatcher = "blocking-dispatcher"

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

      val graph = Source(apps)
        .map(app => (request(app), NotUsed))

      val graph1 = (if (dispatchers.hasDispatcher(blockingDispatcher)) {
        graph.withAttributes(ActorAttributes.dispatcher(blockingDispatcher))
      } else {
        log.warning("Dispatcher '{}' is not found, falling back to default.", blockingDispatcher)
        graph
      })
        .via(poolFlow)
        .flatMapMerge(apps.size, responseValidationFlow)
      val done = graph1.runForeach(replyTo ! GetEventsResponse(_))

      Await.result(done, Duration.Inf)
    }
  }
}
