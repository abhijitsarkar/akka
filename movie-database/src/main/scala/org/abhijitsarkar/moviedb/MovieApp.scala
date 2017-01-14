package org.abhijitsarkar.moviedb

import java.net.URL

import akka.http.scaladsl.Http
import akka.stream._
import akka.stream.scaladsl.{GraphDSL, RunnableGraph, Sink, Source}
import reactivemongo.api.MongoDriver

/**
  * @author Abhijit Sarkar
  */
object MovieApp {
  val driver = MongoDriver()
  val repo = MongoDbMovieRepository(driver)
  val client = OMDbClient()

  val g = (url: String) => RunnableGraph.fromGraph(GraphDSL.create(Sink.ignore) {
    implicit builder =>
      sink =>
        import GraphDSL.Implicits._

        // Source
        import ExcelMovieParser._
        val A: Outlet[(String, String)] = builder.add(Source.fromIterator(() =>
          parseMovies(new URL(url)).iterator)).out
        // Flow
        import MovieService._
        val B: FlowShape[(String, String), Either[String, Movie]] = builder.add(findMovieByTitleAndYear(client))
        // Flow
        val C: FlowShape[Either[String, Movie], Option[String]] = builder.add(persistMovie(repo))

        A ~> B ~> C ~> sink.in

        ClosedShape
  })

  def main(args: Array[String]): Unit = {
    require(args.size >= 1, "File URL is required.")

    g(args(0)).run

    import MovieController._

    Http(system).bindAndHandle(routes(repo, client), config.getString("http.host"), config.getInt("http.port"))
  }
}
