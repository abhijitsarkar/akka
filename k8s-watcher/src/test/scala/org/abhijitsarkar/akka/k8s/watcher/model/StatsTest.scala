package org.abhijitsarkar.akka.k8s.watcher.model

import java.time.temporal.ChronoUnit

import org.scalatest.{FlatSpec, Matchers}

import scala.util.Random

/**
  * @author Abhijit Sarkar
  */
class StatsTest extends FlatSpec with Matchers {
  "Stats" should "keep the startupDurations sorted" in {
    val startupDurations = Random.shuffle((1L to 30L).toList)

    Stats("test", ChronoUnit.SECONDS, startupDurations).startupDurations shouldBe sorted
  }
}
