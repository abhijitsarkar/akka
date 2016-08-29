package name.abhijitsarkar.scauth.model

object OAuthSignatureMethod extends Enumeration {
  type OAuthSignatureMethod = Value
  val HMacSHA1 = Value

  import scala.language.implicitConversions
  implicit def signatureMethodToString(method: OAuthSignatureMethod) = {
    method match {
      case HMacSHA1 => "HMAC-SHA1"
    }
  }
}
