package name.abhijitsarkar.scauth.model

import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{HttpRequest, Uri}
import name.abhijitsarkar.scauth.api.OAuthRequest
import name.abhijitsarkar.scauth.util.SimpleAuthorizationHeaderGenerator.generateAuthorizationHeader
import name.abhijitsarkar.scauth.util.SimpleSignatureGenerator.generateOAuthSignature

case class SimpleOAuthRequest(oAuthCredentials: OAuthCredentials,
    oAuthRequestConfig: OAuthRequestConfig,
    oAuthConfig: OAuthConfig = OAuthConfig()) extends OAuthRequest {

  private var authorizationParams: Map[String, String] = Map("oauth_consumer_key" -> oAuthCredentials.consumerKey,
    "oauth_nonce" -> oAuthConfig.nonceGenerator.generateNonce,
    "oauth_signature_method" -> oAuthConfig.oAuthSignatureMethod,
    "oauth_timestamp" -> oAuthConfig.timestampGenerator.generateTimestampInSeconds,
    "oauth_version" -> oAuthConfig.oAuthVersion)

  oAuthCredentials.token.foreach { v => authorizationParams += ("oauth_token" -> v) }

  // not necessary to include the non OAuth params
  override private[scauth] val signature = generateOAuthSignature(oAuthConfig.oAuthSignatureMethod,
    authorizationParams, oAuthRequestConfig, oAuthCredentials)

  authorizationParams += ("oauth_signature" -> signature)

  private val entityStr = oAuthRequestConfig.entity.fold(identity, right => new String(right.map(_.toChar)))

  override private[scauth] def toHttpRequestWithAuthorizationQueryParams = {
    val queryParams: Map[String, String] = (oAuthRequestConfig.queryParams ++ authorizationParams)
    val uri = Uri(oAuthRequestConfig.baseUrl).withQuery(Query(queryParams))

    HttpRequest(uri = uri, method = oAuthRequestConfig.requestMethod,
      headers = oAuthRequestConfig.headers).withEntity(entityStr)
  }

  override private[scauth] def toHttpRequestWithAuthorizationHeader = {
    val authorizationHeader = generateAuthorizationHeader(authorizationParams, oAuthConfig.oAuthEncoder)
    val uri = Uri(oAuthRequestConfig.baseUrl).withQuery(Query(oAuthRequestConfig.queryParams))

    HttpRequest(uri = uri, method = oAuthRequestConfig.requestMethod,
      headers = oAuthRequestConfig.headers :+ authorizationHeader).withEntity(entityStr)
  }
}