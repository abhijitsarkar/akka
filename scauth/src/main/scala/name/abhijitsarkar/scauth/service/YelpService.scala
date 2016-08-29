package name.abhijitsarkar.scauth.service

import akka.http.scaladsl.unmarshalling.PredefinedFromEntityUnmarshallers.stringUnmarshaller
import name.abhijitsarkar.scauth.model.{OAuthCredentials, OAuthRequestConfig, SimpleOAuthRequest}
import name.abhijitsarkar.scauth.util.ActorPlumbing

class YelpService(val oAuthCredentials: OAuthCredentials)(implicit val actorPlumbing: ActorPlumbing) {
  private val baseUri = "http://api.yelp.com"
  private val searchUri = s"${baseUri}/v2/search"
  private val resultLimit = "3"
  private val oAuthService = new SimpleOAuthService[String]()

  def searchForBusinessesByLocation(searchTerm: String, location: String) = {
    val queryParams = Map("term" -> searchTerm, "location" -> location, "limit" -> resultLimit)

    val oAuthRequestConfig = OAuthRequestConfig(baseUrl = searchUri, queryParams = queryParams)
    
    val request = SimpleOAuthRequest(oAuthCredentials, oAuthRequestConfig)

    oAuthService.sendWithAuthorizationQueryParams(request)
  }
}