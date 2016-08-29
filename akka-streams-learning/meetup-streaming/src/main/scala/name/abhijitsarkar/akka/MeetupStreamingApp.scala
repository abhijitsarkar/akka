package name.abhijitsarkar.akka

import akka.actor.ActorSystem
import akka.stream.scaladsl.{GraphDSL, Sink, UnzipWith}
import akka.stream.{ActorMaterializer, SinkShape}
import akka.util.ByteString
import name.abhijitsarkar.akka.model.Rsvp
import name.abhijitsarkar.akka.service.{MeetupStreamingService, RsvpSubscriber}
import name.abhijitsarkar.akka.util.ActorPlumbing

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

object MeetupStreamingApp extends App {
  implicit val system = ActorSystem("twitter")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = {
    implicitly
  }

  implicit val actorPlumbing: ActorPlumbing = ActorPlumbing()

  val firstSubscriber = Sink.actorSubscriber(RsvpSubscriber.props("first"))
  val secondSubscriber = Sink.actorSubscriber(RsvpSubscriber.props("second"))

  val rsvpSink = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val splitStream = builder.add(UnzipWith[ByteString, Rsvp, Rsvp] { byteStr =>
      import model.RsvpJsonSupport._
      import spray.json._

      val rsvp = byteStr.utf8String.parseJson.convertTo[Rsvp]

      (rsvp, rsvp)
    })

    /* Broadcast could be used too */

    //    val rsvpFlow: Flow[ByteString, Rsvp, NotUsed] = Flow[ByteString].map {
    //      import model.RsvpJsonSupport._
    //      import spray.json._
    //
    //      _.utf8String.parseJson.convertTo[Rsvp]
    //    }
    //
    //    val broadcast = builder.add(Broadcast[Rsvp](2))
    //
    //    val rsvp = builder.add(rsvpFlow)
    //
    //    broadcast ~> firstSubscriber
    //    broadcast ~> secondSubscriber
    //
    //    rsvp ~> broadcast
    //
    //    SinkShape(rsvp.in)

    splitStream.out0 ~> firstSubscriber
    splitStream.out1 ~> secondSubscriber

    SinkShape(splitStream.in)
  }

  val meetupStreamingService = new MeetupStreamingService(rsvpSink)

  meetupStreamingService.stream
}