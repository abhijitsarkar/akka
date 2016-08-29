package name.abhijitsarkar.scauth.service

import akka.NotUsed
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream.scaladsl.Source
import akka.stream.{Graph, SinkShape}
import akka.util.ByteString
import name.abhijitsarkar.scauth.model.{OAuthCredentials, OAuthRequestConfig, SimpleOAuthRequest}
import name.abhijitsarkar.scauth.util.ActorPlumbing
import org.slf4j.LoggerFactory

class TwitterStreamingService[T](val oAuthCredentials: OAuthCredentials,
                                 val partial: Graph[SinkShape[ByteString], NotUsed])(implicit val actorPlumbing: ActorPlumbing) {
  private val log = LoggerFactory.getLogger(getClass())

  private val baseUri = "https://stream.twitter.com/1.1"
  private val streamingUri = s"${baseUri}/statuses/filter.json"

  import actorPlumbing._

  def stream(follow: Option[String], track: Option[String]) = {
    val httpRequest = this.httpRequest {
      queryParams(follow, track)
    }
    val flow = this.flow {
      httpRequest
    }

    Source.single(httpRequest -> 42)
      .via(flow)
      .flatMapConcat(_._1.get.entity.dataBytes)
      .runWith(partial)
  }

  private def queryParams(follow: Option[String], track: Option[String]) = {
    (follow, track) match {
      case (Some(a), Some(b)) => Map("follow" -> a, "track" -> b)
      case (Some(a), None) => Map("follow" -> a)
      case (None, Some(b)) => Map("track" -> b)
      case _ => throw new IllegalArgumentException("One of 'follow' and 'track' parameters must be specified.")
    }
  }

  private def httpRequest(queryParams: Map[String, String]) = {
    val oAuthRequestConfig = OAuthRequestConfig(baseUrl = streamingUri, queryParams = queryParams)
    val request = SimpleOAuthRequest(oAuthCredentials, oAuthRequestConfig)
    val httpRequest = request.toHttpRequestWithAuthorizationQueryParams

    log.debug(s"Http request: {}.", httpRequest)

    httpRequest
  }

  def flow(httpRequest: HttpRequest) = {
    val host = httpRequest.uri.authority.host.address()
    Http().newHostConnectionPoolHttps[Int](host)
  }
}