package org.abhijitsarkar.moviedb

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream._
import akka.stream.scaladsl.{GraphDSL, RunnableGraph, Sink, Source}
import com.typesafe.config.Config
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.{FailoverStrategy, MongoConnectionOptions, MongoDriver}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * @author Abhijit Sarkar
  */
object MovieApp extends MovieService {
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
      .onComplete(_ => Await.result(system.terminate(), 5.seconds))
  }

  override implicit val system = ActorSystem("MovieApp")
  override implicit val executor: ExecutionContext = system.dispatcher
  override implicit val materializer: Materializer = ActorMaterializer()

  override def config: Config = system.settings.config

  val opts = MongoConnectionOptions(
    failoverStrategy = FailoverStrategy(retries = 2)
  )

  import scala.collection.JavaConverters._

  override val movieCollection: Future[BSONCollection] = {
    val mongodbConfig = config.getConfig("mongodb")
    MongoDriver().connection(mongodbConfig.getStringList("servers").asScala, opts)
      .database(mongodbConfig.getString("database"))
      .map(_.collection(mongodbConfig.getString("collection")))
  }

  override val logger: LoggingAdapter = Logging.getLogger(system, this)
}
