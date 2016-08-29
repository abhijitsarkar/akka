package name.abhijitsarkar.scauth.model

import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import name.abhijitsarkar.scauth.api.{OAuth10Signature, OAuthEncoder}
import name.abhijitsarkar.scauth.util.SimpleUrlEncoder

case class HmacSHA1Signature(oAuthRequestConfig: OAuthRequestConfig,
  consumerSecret: String, tokenSecret: Option[String] = None,
  val oAuthEncoder: OAuthEncoder = SimpleUrlEncoder)
    extends OAuth10Signature(oAuthRequestConfig, consumerSecret, tokenSecret,
      oAuthEncoder) {

  override def newInstance = {
    val signingKey = super.signingKey

    val secret = new SecretKeySpec(signingKey.getBytes, "HmacSHA1")
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(secret)
    val result: Array[Byte] = mac.doFinal(this.baseString.getBytes)

    Base64.getEncoder.encodeToString(result)
  }
}