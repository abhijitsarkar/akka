package name.abhijitsarkar.akka

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString
import akka.{Done, NotUsed}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.forkjoin.ThreadLocalRandom
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

// Originally taken from https://github.com/typesafehub/activator-akka-stream-scala/blob/master/src/main/scala/sample/stream/WritePrimes.scala
// Modified to fit latest API
object WritePrimes {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("Sys")
    implicit val materializer = ActorMaterializer()

    // generate random numbers
    val maxRandomNumberSize = 100000
    val primeSource: Source[Int, NotUsed] =
      Source.fromIterator[Int](() => Iterator.continually(ThreadLocalRandom.current().nextInt(maxRandomNumberSize))).
        // filter prime numbers
        filter(rnd => isPrime(rnd)).
        // and neighbor +2 is also prime
        filter(prime => isPrime(prime + 2))

    // write to file sink
    val fileSink: Sink[ByteString, Future[IOResult]] = FileIO.toPath(Paths.get("target/primes.txt"))
    val flow: Flow[Int, ByteString, NotUsed] = Flow[Int]
      // act as if processing is really slow
      .map(i => {
      Thread.sleep(1000);
      ByteString(i.toString)
    })
    val slowSink: Sink[Int, Future[IOResult]] = flow.toMat(fileSink)(Keep.right)

    // console output sink
    val consoleSink: Sink[Int, Future[Done]] = Sink.foreach[Int](println)

    val g = RunnableGraph.fromGraph(GraphDSL.create(slowSink, consoleSink)((_, _)) { implicit builder =>
      (s, c) =>
        import GraphDSL.Implicits._

        val broadcast: UniformFanOutShape[Int, Int] = builder.add(Broadcast[Int](2)) // the splitter - like a Unix tee
        primeSource ~> broadcast ~> s // connect primes to splitter, and one side to file
        broadcast ~> c // connect other side of splitter to console

        ClosedShape
    })

    import scala.concurrent.duration._

    g.run()
      ._2.onComplete {
      case Success(_) =>
        Await.result(system.terminate(), 10.seconds)
      case Failure(e) =>
        println(s"Failure: ${e.getMessage}")
        Await.result(system.terminate(), 10.seconds)
    }
  }

  def isPrime(n: Int): Boolean = {
    if (n <= 1) false
    else if (n == 2) true
    else !(2 to (n - 1)).exists(x => n % x == 0)
  }
}
