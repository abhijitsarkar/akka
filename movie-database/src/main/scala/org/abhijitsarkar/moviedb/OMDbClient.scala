package org.abhijitsarkar.moviedb

import java.io.IOException

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, ResponseEntity, Uri}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.typesafe.config.Config
import org.abhijitsarkar.moviedb.MovieProtocol._
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Abhijit Sarkar
  */

trait OMDbClient {
  implicit val system: ActorSystem

  implicit val executor: ExecutionContext

  implicit val materializer: Materializer

  implicit val movieUnmarshaller = Unmarshaller[ResponseEntity, Movie](ec => r => {
    val str = r.dataBytes.runFold("")((u, b) => s"$u${b.utf8String}")

    str.map(_.parseJson.convertTo[Movie])
  })

  def config: Config

  val logger: LoggingAdapter

  lazy val omdbConnectionFlow: Flow[HttpRequest, HttpResponse, Any] =
    Http().outgoingConnection(config.getString("omdb.host"), config.getInt("omdb.port"))

  def omdbRequest(request: HttpRequest): Future[HttpResponse] = Source.single(request).via(omdbConnectionFlow).runWith(Sink.head)

  def movieInfo(title: String, year: Int): Future[Either[String, Movie]] = {
    val uri = Uri("/").withQuery(Query(("t" -> title), ("y" -> year.toString)))
    omdbRequest(RequestBuilding.Get(uri)).flatMap { response =>
      response.status match {
        case OK => Unmarshal(response.entity).to[Movie].map(Right(_))
        case _ => Unmarshal(response.entity).to[String].flatMap { entity =>
          val error = s"OMDb request failed with status code ${response.status} and entity $entity."
          logger.error(error)
          Future.failed(new IOException(error))
        }
      }
    }
  }
}
