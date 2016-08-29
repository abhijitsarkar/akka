package name.abhijitsarkar.scauth.api

import akka.http.scaladsl.model.HttpRequest

trait OAuthRequest {
  private[scauth] def toHttpRequestWithAuthorizationQueryParams: HttpRequest
  private[scauth] def toHttpRequestWithAuthorizationHeader: HttpRequest
  
  private[scauth] val signature: String
}
