package name.abhijitsarkar.scauth.service

import akka.http.scaladsl.unmarshalling.PredefinedFromEntityUnmarshallers.stringUnmarshaller
import name.abhijitsarkar.scauth.util.SimpleUrlEncoder
import name.abhijitsarkar.scauth.model.{OAuthCredentials, OAuthRequestConfig, SimpleOAuthRequest}
import name.abhijitsarkar.scauth.util.{ActorPlumbing, SimpleUrlEncoder}

class TwitterSearchService(val oAuthCredentials: OAuthCredentials)(implicit val actorPlumbing: ActorPlumbing) {
  private val baseUri = "https://api.twitter.com/1.1"
  private val searchUri = s"${baseUri}/search/tweets.json"
  private val resultLimit = "3"
  private val oAuthService = new SimpleOAuthService[String]()

  // Twitter search API - https://dev.twitter.com/rest/public/search
  // Various Twitter search parameters - https://dev.twitter.com/rest/reference/get/search/tweets
  def search(query: String, resultType: String = "mixed") = {
    // https://github.com/akka/akka/issues/18574
    val queryParams = Map("q" -> SimpleUrlEncoder.encode(query), "result_type" -> resultType, "count" -> resultLimit)

    val oAuthRequestConfig = OAuthRequestConfig(baseUrl = searchUri, queryParams = queryParams)

    val request = SimpleOAuthRequest(oAuthCredentials, oAuthRequestConfig)

    oAuthService.sendWithAuthorizationHeader(request)
  }
}