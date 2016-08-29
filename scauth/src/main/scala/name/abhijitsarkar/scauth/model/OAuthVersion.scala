package name.abhijitsarkar.scauth.model

object OAuthVersion extends Enumeration {
  type OAuthVersion = Value
  val OneOh = Value

  import scala.language.implicitConversions
  implicit def versionToString(version: OAuthVersion) = {
    version match {
      case OneOh => "1.0"
    }
  }
}
