package name.abhijitsarkar.scauth.api

import akka.http.scaladsl.model.{HttpRequest, ResponseEntity}
import akka.http.scaladsl.unmarshalling.Unmarshaller

import scala.concurrent.Future

trait OAuthService[A] {
  def sendWithAuthorizationQueryParams(request: OAuthRequest)(implicit unmarshaller: Unmarshaller[ResponseEntity, A]): Future[A] = {
    val httpRequest = request.toHttpRequestWithAuthorizationQueryParams

    sendAndReceive(httpRequest, request.signature)
  }

  def sendWithAuthorizationHeader(request: OAuthRequest)(implicit unmarshaller: Unmarshaller[ResponseEntity, A]): Future[A] = {
    val httpRequest = request.toHttpRequestWithAuthorizationHeader

    sendAndReceive(httpRequest, request.signature)
  }

  protected def sendAndReceive(httpRequest: HttpRequest, id: String)(implicit unmarshaller: Unmarshaller[ResponseEntity, A]): Future[A]
}
