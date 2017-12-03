package org.abhijitsarkar.akka.k8s.watcher.model

import java.time.temporal.ChronoUnit
import java.util.LongSummaryStatistics
import java.util.stream.Collectors.summarizingLong

import scala.collection.JavaConverters._

/**
  * @author Abhijit Sarkar
  */
case class Stats(
                  app: String,
                  unit: ChronoUnit,
                  startupDurations: List[Long]
                ) {
  val summary: LongSummaryStatistics = {
    startupDurations.asJava.stream()
      .collect(summarizingLong(identity[Long]))
  }
}
