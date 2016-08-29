package name.abhijitsarkar.akka.service

import akka.NotUsed
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.Uri.apply
import akka.stream.scaladsl.Source
import akka.stream.{Graph, SinkShape}
import akka.util.ByteString
import name.abhijitsarkar.akka.util.ActorPlumbing
import org.slf4j.LoggerFactory

class MeetupStreamingService(val sink: Graph[SinkShape[ByteString], NotUsed])(implicit val actorPlumbing: ActorPlumbing) {
  private val log = LoggerFactory.getLogger(getClass())

  private val baseUri = "http://stream.meetup.com/2/rsvps"

  import actorPlumbing._

  def stream = {
    val httpRequest = HttpRequest(uri = baseUri, method = GET)

    val flow = {
      val host = httpRequest.uri.authority.host.address()
      Http().newHostConnectionPoolHttps[Int](host)
    }

    Source.single(httpRequest -> 42)
      .via(flow)
      .flatMapConcat(_._1.get.entity.dataBytes)
      .runWith(sink)
  }
}