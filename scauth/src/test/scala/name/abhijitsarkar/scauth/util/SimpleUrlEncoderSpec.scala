package name.abhijitsarkar.scauth.util

import name.abhijitsarkar.scauth.util.SimpleUrlEncoder._
import org.scalatest.{FlatSpec, Matchers}

class SimpleUrlEncoderSpec extends FlatSpec with Matchers {
  it should "replace + in encoded string" in {
    val encoded = encode("Hello Ladies + Gentlemen, a signed OAuth request!")
    
    encoded shouldBe("Hello%20Ladies%20%2B%20Gentlemen%2C%20a%20signed%20OAuth%20request%21")
  }
}