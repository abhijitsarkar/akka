package org.abhijitsarkar.moviedb

import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, ResponseEntity, Uri}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.scaladsl.{Flow, Sink, Source}
import org.abhijitsarkar.moviedb.MovieProtocol._
import spray.json._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * @author Abhijit Sarkar
  */
trait OMDbClient {
  implicit val movieUnmarshaller = Unmarshaller[ResponseEntity, Either[String, Movie]](ec => r => {
    val str = r.dataBytes.runFold("")((u, b) => s"$u${b.utf8String}")

    str.map { s =>
      logger.info(s"OMDb response: $str")
      Try(s.parseJson.convertTo[Movie]) match {
        case Success(m) => Right(m)
        case Failure(t) => logger.error(t, t.getMessage); Left(t.getMessage)
      }
    }
  })

  lazy val omdbConnectionFlow: Flow[HttpRequest, HttpResponse, Any] =
    Http().outgoingConnection(config.getString("omdb.host"), config.getInt("omdb.port"))

  def omdbRequest(request: HttpRequest): Future[HttpResponse] = Source.single(request).via(omdbConnectionFlow).runWith(Sink.head)

  def movieInfo(title: String, year: String): Future[Either[String, Movie]] = {
    val uri = Uri("/").withQuery(Query(("t" -> title), ("y" -> year.toString)))
    logger.info(s"OMDb request: $uri")

    omdbRequest(RequestBuilding.Get(uri)).flatMap { response =>
      response.status match {
        case OK => Unmarshal(response.entity).to[Either[String, Movie]]
        case _ => Unmarshal(response.entity).to[String].flatMap { entity =>
          val error = s"OMDb request failed with status code ${response.status} and entity $entity."
          logger.error(error)
          Future.failed(new RuntimeException(error))
        }
      }
    }
  }
}
