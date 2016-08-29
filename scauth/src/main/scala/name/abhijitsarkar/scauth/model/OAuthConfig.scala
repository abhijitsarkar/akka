package name.abhijitsarkar.scauth.model

import name.abhijitsarkar.scauth.api.OAuthEncoder
import name.abhijitsarkar.scauth.model.OAuthSignatureMethod.OAuthSignatureMethod
import name.abhijitsarkar.scauth.model.OAuthVersion.OAuthVersion
import name.abhijitsarkar.scauth.util._
import name.abhijitsarkar.scauth.model.OAuthVersion._
import name.abhijitsarkar.scauth.model.OAuthSignatureMethod._

case class OAuthConfig(oAuthVersion: OAuthVersion = OneOh, oAuthSignatureMethod: OAuthSignatureMethod = HMacSHA1,
  nonceGenerator: NonceGenerator = SimpleNonceGenerator,
  timestampGenerator: TimestampGenerator = SimpleTimestampGenerator,
  oAuthEncoder: OAuthEncoder = SimpleUrlEncoder)