package name.abhijitsarkar.akka

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink}
import akka.stream.{ActorMaterializer, OverflowStrategy, ThrottleMode}
import akka.util.ByteString
import name.abhijitsarkar.akka.Backpressure._
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * @author Abhijit Sarkar
  */
class BackpressureSpec extends FlatSpec with Matchers {
  implicit val system = ActorSystem("backpressure")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  def filesink(filename: String) = {
    Flow[Int]
      .alsoTo(Sink.foreach(s => println(s"$filename: $s")))
      .map(s => ByteString(s + "\n"))
      .toMat(FileIO.toPath(Paths.get(s"random-examples/target/$filename")))(Keep.right)
  }

  "backpressure" should "process normally when fast producer, fast consumers" in {
    val sink1 = Flow[Int]
      .toMat(filesink("out1.txt"))(Keep.right)
    val sink2 = Flow[Int]
      .toMat(filesink("out2.txt"))(Keep.right)

    val result = build(sink1, sink2).run

    val flattened = result._1.flatMap(_ => result._2)

    Await.result(flattened, 5.minutes)

    Await.result(system.terminate(), 5.minutes)
  }

  it should "slow down producer when one consumer is slow" in {
    val slowSink = Flow[Int]
      .via(Flow[Int]
        .throttle(1, 1.second, 1, ThrottleMode.shaping))
      .toMat(filesink("out1.txt"))(Keep.right)
    val fastSink = Flow[Int]
      .toMat(filesink("out2.txt"))(Keep.right)

    val result = build(slowSink, fastSink).run

    val flattened = result._1.flatMap(_ => result._2)

    Await.result(flattened, 5.minutes)

    Await.result(system.terminate(), 5.minutes)
  }

  it should "not slow down fast consumer when one slow consumer is buffering with dropNew" in {
    val slowSink = Flow[Int]
      .buffer(50, OverflowStrategy.dropNew)
      .via(Flow[Int]
        .throttle(1, 1.second, 1, ThrottleMode.shaping))
      .toMat(filesink("out1.txt"))(Keep.right)
    val fastSink = Flow[Int]
      .toMat(filesink("out2.txt"))(Keep.right)

    val result = build(slowSink, fastSink).run

    val flattened = result._1.flatMap(_ => result._2)

    Await.result(flattened, 5.minutes)

    Await.result(system.terminate(), 5.minutes)
  }

  it should "not slow down producer *until* slow consumer buffer is full" in {
    val slowSink = Flow[Int]
      .buffer(50, OverflowStrategy.backpressure)
      .via(Flow[Int]
        .throttle(1, 1.second, 1, ThrottleMode.shaping))
      .toMat(filesink("out1.txt"))(Keep.right)
    val fastSink = Flow[Int]
      .toMat(filesink("out2.txt"))(Keep.right)

    val result = build(slowSink, fastSink).run

    val flattened = result._1.flatMap(_ => result._2)

    Await.result(flattened, 5.minutes)

    Await.result(system.terminate(), 5.minutes)
  }
}
