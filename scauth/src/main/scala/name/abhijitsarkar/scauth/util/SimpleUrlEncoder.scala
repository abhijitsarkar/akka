package name.abhijitsarkar.scauth.util

import java.net.URLEncoder
import java.nio.charset.Charset

import name.abhijitsarkar.scauth.api.OAuthEncoder

case object SimpleUrlEncoder extends OAuthEncoder {
  override val encodingOverrides = Map[String, String]("\\*" -> "%2A", "\\+" -> "%20", "%7E" -> "~")

  override def encode(plainText: String, charset: Charset) = {
    super.postProcess {
      URLEncoder.encode(plainText, charset.name)
    }
  }
}