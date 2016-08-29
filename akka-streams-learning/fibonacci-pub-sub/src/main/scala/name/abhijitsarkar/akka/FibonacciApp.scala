package name.abhijitsarkar.akka

import akka.actor.{ActorSystem, Props}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Sink, Source}
import akka.stream.{ActorMaterializer, SinkShape}

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

object FibonacciApp extends App {
  implicit val system = ActorSystem("fibonacci")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = {
    implicitly
  }

  // build graph
  val evenSubscriber = Sink.actorSubscriber(FibonacciSubscriber.props("even"))
  val oddSubscriber = Sink.actorSubscriber(FibonacciSubscriber.props("odd"))

  val evenSink = Flow[Long].filter {
    _ % 2 == 0
  }.toMat(evenSubscriber)(Keep.left)
  val oddSink = Flow[Long].filter {
    _ % 2 != 0
  }.toMat(oddSubscriber)(Keep.left)

  val g = GraphDSL.create(evenSink, oddSink)((_, _)) { implicit builder =>
    (even, odd) =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[Long](2))

      broadcast ~> even.in
      broadcast ~> odd.in

      SinkShape(broadcast.in)
  }

  // run graph
  val src = Source.actorPublisher(Props[FibonacciPublisher])
  src.runWith(g)
}
