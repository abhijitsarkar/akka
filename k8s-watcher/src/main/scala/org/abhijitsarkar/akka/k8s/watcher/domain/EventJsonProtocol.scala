package org.abhijitsarkar.akka.k8s.watcher.domain

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.abhijitsarkar.akka.k8s.watcher.domain.PodPhase.PodPhase
import spray.json.{DefaultJsonProtocol, DeserializationException, JsString, JsValue, RootJsonFormat, _}

/**
  * @author Abhijit Sarkar
  */

object EventJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {

  implicit object InstantJsonFormat extends RootJsonFormat[Instant] {
    override def write(obj: Instant): JsValue = DateTimeFormatter.ISO_INSTANT
      .withZone(ZoneId.of("UTC"))
      .format(obj).toJson

    override def read(json: JsValue): Instant = {
      json match {
        case JsString(x) => Instant.parse(x)
        case somethingElse => throw new DeserializationException(s"Can't deserialize event $somethingElse.")
      }
    }
  }

  implicit val podConditionStatusConverter = new EnumJsonConverter(PodConditionStatus)
  implicit val podConditionStatusTypeConverter = new EnumJsonConverter(PodConditionStatusType)
  implicit val podPhaseConverter = new EnumJsonConverter(PodPhase)
  implicit val eventTypeConverter = new EnumJsonConverter(EventType)

  implicit val podConditionFormat = jsonFormat4(PodCondition)
  implicit val metadataFormat = jsonFormat4(Metadata)

  implicit object PodStatusJsonFormat extends RootJsonFormat[PodStatus] {
    override def write(status: PodStatus): JsValue = JsObject(
      "phase" -> status.phase.toJson,
      "conditions" -> status.conditions.toJson
    )

    override def read(json: JsValue): PodStatus = json.asJsObject.getFields("phase", "conditions") match {
      case Seq(
      phase,
      conditions
      ) => PodStatus(phase.convertTo[PodPhase], conditions.convertTo[List[PodCondition]])
      case Seq(
      phase
      ) => PodStatus(phase = phase.convertTo[PodPhase])
    }
  }

  implicit val podFormat = jsonFormat2(Pod)
  implicit val eventFormat = jsonFormat2(Event)

}

// https://github.com/spray/spray-json/issues/200
class EnumJsonConverter[T <: scala.Enumeration](enu: T) extends RootJsonFormat[T#Value] {
  override def write(obj: T#Value): JsValue = JsString(obj.toString)

  override def read(json: JsValue): T#Value = {
    json match {
      case JsString(txt) => enu.withName(txt)
      case somethingElse => throw DeserializationException(s"Can't deserialize $enu from $somethingElse.")
    }
  }
}
