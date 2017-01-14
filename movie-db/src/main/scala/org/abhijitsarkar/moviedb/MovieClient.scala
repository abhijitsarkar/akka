package org.abhijitsarkar.moviedb

import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.scaladsl.{Flow, Sink, Source}
import cats.data.EitherT
import org.abhijitsarkar.moviedb.MovieProtocol._
import spray.json._
import cats.instances.future._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * @author Abhijit Sarkar
  */
trait MovieClient {
  def findByTitleAndYear(title: String, year: String): Future[Either[String, Movie]]

  def findById(id: String): Future[Either[String, Movie]]
}

class OMDbClient extends MovieClient {
  private implicit val movieUnmarshaller = Unmarshaller[ResponseEntity, Either[String, Movie]](ec => r => {
    val str = r.dataBytes.runFold("")((u, b) => s"$u${b.utf8String}")

    str.map { s =>
      Try(s.parseJson.convertTo[Movie]) match {
        case Success(m) => Right(m)
        case Failure(t) => logger.error(t, s"Failed to unmarshal OMDb response: $str"); Left(t.getMessage)
      }
    }
  })

  private lazy val omdbConnectionFlow: Flow[HttpRequest, HttpResponse, Any] =
    Http().outgoingConnection(config.getString("omdb.host"), config.getInt("omdb.port"))

  private def omdbRequest(request: HttpRequest): Future[HttpResponse] = Source.single(request)
    .via(omdbConnectionFlow)
    .runWith(Sink.head)

  private def responseMatcher(response: HttpResponse) = PartialFunction[StatusCode, Future[Either[String, Movie]]] {
    case OK => Unmarshal(response.entity).to[Either[String, Movie]]
    case _ => Unmarshal(response.entity).to[String].flatMap { entity =>
      val error = s"OMDb request failed with status code ${response.status} and entity $entity."
      logger.error(error)
      Future.failed(new RuntimeException(error))
    }
  }

  override def findByTitleAndYear(title: String, year: String): Future[Either[String, Movie]] = {
    val uri = Uri("/").withQuery(Query(("t" -> title), ("y" -> year.toString)))
    logger.info(s"OMDb request: $uri")

    omdbRequest(RequestBuilding.Get(uri))
      .flatMap { response =>
        EitherT(responseMatcher(response)(response.status)).leftMap(msg => {
          logger.error(s"Failed to find a movie with title: $title and year: $year")
          msg
        }).value
      }
  }

  override def findById(id: String): Future[Either[String, Movie]] = {
    val uri = Uri("/").withQuery(Query(("i" -> id)))
    logger.info(s"OMDb request: $uri")

    omdbRequest(RequestBuilding.Get(uri))
      .flatMap { response =>
        EitherT(responseMatcher(response)(response.status)).leftMap(msg => {
          logger.error(s"Failed to find a movie with id: $id")
          msg
        }).value
      }
  }
}

object OMDbClient {
  def apply() = new OMDbClient
}
