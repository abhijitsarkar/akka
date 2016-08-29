package name.abhijitsarkar.scauth.util

import name.abhijitsarkar.scauth.model.OAuthSignatureMethod._
import name.abhijitsarkar.scauth.model.{HmacSHA1Signature, OAuthCredentials, OAuthRequestConfig}

trait SignatureGenerator {
  def generateOAuthSignature(oAuthSignatureMethod: OAuthSignatureMethod, authorizationParams: Map[String, String],
    oAuthRequestConfig: OAuthRequestConfig, oAuthCredentials: OAuthCredentials): String
}

object SimpleSignatureGenerator extends SignatureGenerator {
  override def generateOAuthSignature(oAuthSignatureMethod: OAuthSignatureMethod, authorizationParams: Map[String, String],
    oAuthRequestConfig: OAuthRequestConfig, oAuthCredentials: OAuthCredentials) = {
    val queryParams = oAuthRequestConfig.queryParams ++ authorizationParams

    oAuthSignatureMethod match {
      case HMacSHA1 => HmacSHA1Signature(oAuthRequestConfig.copy(
        queryParams = queryParams),
        consumerSecret = oAuthCredentials.consumerSecret,
        tokenSecret = oAuthCredentials.tokenSecret).newInstance
    }
  }
}