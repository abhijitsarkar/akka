package name.abhijitsarkar.akka

import akka.stream.scaladsl.{Broadcast, GraphDSL, RunnableGraph, Sink, Source}
import akka.stream.{ClosedShape, IOResult}

import scala.concurrent.Future

/**
  * Adopted from http://chariotsolutions.com/blog/post/simply-explained-akka-streams-backpressure
  * Updated to demonstrate various scenarios discussed in the blog.
  *
  * @see BackpressureSpec
  * @author Abhijit Sarkar
  */
object Backpressure {
  def build(sink1: Sink[Int, Future[IOResult]], sink2: Sink[Int, Future[IOResult]]) = {
    val source = Source(1 to 10000)

    RunnableGraph.fromGraph(GraphDSL.create(sink1, sink2)((_, _)) { implicit b =>
      (s1, s2) =>
        import GraphDSL.Implicits._

        val bcast = b.add(Broadcast[Int](2))
        source ~> bcast.in
        bcast.out(0) ~> s1.in
        bcast.out(1) ~> s2.in

        ClosedShape
    })
  }
}
