package name.abhijitsarkar.scauth.model

case class OAuthCredentials(consumerKey: String, consumerSecret: String,
  token: Option[String] = None, tokenSecret: Option[String] = None)