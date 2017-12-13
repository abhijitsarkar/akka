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
                  // The private keyword keeps Scala from exposing that field to other classes,
                  // and the var lets the value of the field be changed.
                  private var _startupDurations: List[Long]
                ) {
  def startupDurations = _startupDurations.sorted

  def startupDurations_=(durations: List[Long]) = _startupDurations = durations

  val summary: LongSummaryStatistics = {
    _startupDurations.asJava.stream()
      .collect(summarizingLong(identity[Long]))
  }
}
