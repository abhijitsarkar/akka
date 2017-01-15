package org.abhijitsarkar.moviedb

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Sink}

import scala.concurrent.Future

/**
  * @author Abhijit Sarkar
  */
trait MovieService {
  def repo: MovieRepository

  //  val cores = Runtime.getRuntime.availableProcessors

  def findMovieById(id: String): Future[Option[Movie]] = repo.findById(id)

  def deleteMovie(id: String): Future[Option[String]] = repo.delete(id)

  def persistMovies: Flow[Either[String, Movie], Future[Int], NotUsed] = {
    Flow.fromGraph(
      GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._
        val bcast = builder.add(Broadcast[Either[String, Movie]](2))

        val error = Flow[Either[String, Movie]]
          .filter(_.isLeft)
          .map(_.left.get)
          .to(Sink.foreach(logger.error))
        val ok = Flow[Either[String, Movie]]
          .filter(_.isRight)
          .map(_.right.get)
          .grouped(50)
          .map(repo.create)

        val okShape = builder.add(ok)

        bcast ~> builder.add(error)
        bcast ~> okShape

        FlowShape(bcast.in, okShape.out)
      }
    )
  }

  def client: MovieClient

  // capped at 20
  def findMovieByTitleAndYear: Flow[(String, String), Either[String, Movie], NotUsed] = Flow[(String, String)]
    .mapAsyncUnordered(16)(x => (client.findByTitleAndYear _).tupled(x))

  def findMovieByImdbId(id: String): Future[Either[String, Movie]] = client.findById(id)
}
