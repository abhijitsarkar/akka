package name.abhijitsarkar.akka

import org.scalatest.{FlatSpec, Matchers}

/**
  * @author Abhijit Sarkar
  */
class NoaaCurrentConditionsClientSpec extends FlatSpec with Matchers {
  "NoaaCurrentConditionsClient" should "download and extract current condition files" in {
    println(NoaaCurrentConditionsClient.currentConditionsPath())
  }
}
