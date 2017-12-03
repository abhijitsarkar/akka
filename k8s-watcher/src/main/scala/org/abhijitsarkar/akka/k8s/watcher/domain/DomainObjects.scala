package org.abhijitsarkar.akka.k8s.watcher.domain

import java.time.temporal.ChronoUnit
import java.time.{Duration, Instant}

import org.abhijitsarkar.akka.k8s.watcher.domain.EventType.EventType
import org.abhijitsarkar.akka.k8s.watcher.domain.PodConditionStatus.PodConditionStatus
import org.abhijitsarkar.akka.k8s.watcher.domain.PodConditionStatusType.{Initialized, PodConditionStatusType, Ready}
import org.abhijitsarkar.akka.k8s.watcher.domain.PodPhase.PodPhase

import scala.collection.immutable.Map

/**
  * @author Abhijit Sarkar
  */
case class Event(
                  `type`: EventType,
                  `object`: Pod
                )

case class Pod(
                metadata: Metadata,
                status: PodStatus
              ) {
  def app = metadata.labels.get("app")

  def uid = metadata.uid

  def ready = status.conditions.exists(
    c => c.`type` == Ready
      && c.status == PodConditionStatus.True
  )

  def startupDuration(unit: ChronoUnit): Long = {
    val map = status.conditions
      .filter(c => c.`type` == Initialized || c.`type` == Ready)
      .map(c => (c.`type`, c.lastTransitionTime))
      .toMap

    map.get(Initialized).zip(map.get(Ready)) match {
      case Nil => -1L
      case List((initialized, ready)) => Duration.between(ready, initialized).abs().get(unit)
    }
  }
}

case class Metadata(
                     uid: String,
                     namespace: String,
                     name: String,
                     labels: Map[String, String]
                   )

case class PodStatus(
                      phase: PodPhase,
                      conditions: List[PodCondition] = Nil
                    )

case class PodCondition(
                         `type`: PodConditionStatusType,
                         status: PodConditionStatus,
                         lastProbeTime: Option[Instant],
                         lastTransitionTime: Instant
                       )

object EventType extends Enumeration {
  type EventType = Value
  val ADDED, MODIFIED, DELETED = Value
}

object PodPhase extends Enumeration {
  type PodPhase = Value
  val Pending, Running, Succeeded, Failed, Unknown = Value
}

object PodConditionStatusType extends Enumeration {
  type PodConditionStatusType = Value
  val PodScheduled, Ready, Initialized, Unschedulable = Value
}

object PodConditionStatus extends Enumeration {
  type PodConditionStatus = Value
  val True, False, Unknown = Value
}