package org.abhijitsarkar.moviedb

import akka.stream._
import akka.stream.scaladsl.{GraphDSL, RunnableGraph, Sink, Source}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * @author Abhijit Sarkar
  */
object MovieApp extends MovieService with MovieRepositoryHelper {
  val g = (file: String) => RunnableGraph.fromGraph(GraphDSL.create(Sink.ignore) {
    implicit builder =>
      sink =>
        import GraphDSL.Implicits._

        // Source
        val A: Outlet[(String, String)] = builder.add(Source.fromIterator(() => parseMovies(file).iterator)).out
        // Flow
        val B: FlowShape[(String, String), Either[String, Movie]] = builder.add(findMovie)
        // Flow
        val C: FlowShape[Either[String, Movie], Option[String]] = builder.add(persistMovie)

        A ~> B ~> C ~> sink.in

        ClosedShape
  })

  def main(args: Array[String]): Unit = {
    require(args.size >= 1, "Path to file is required.")

    g(args(0)).run
      .onComplete(_ => {
        driver.close(5.seconds)
        Await.result(system.terminate(), 5.seconds)
      })
  }
}