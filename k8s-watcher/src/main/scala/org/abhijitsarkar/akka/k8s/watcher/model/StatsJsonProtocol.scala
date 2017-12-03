package org.abhijitsarkar.akka.k8s.watcher.model

import java.util.LongSummaryStatistics

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, JsArray, JsNumber, JsObject, JsString, JsValue, RootJsonFormat, _}

/**
  * @author Abhijit Sarkar
  */

object StatsJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {

  implicit object SummaryJsonFormat extends RootJsonFormat[LongSummaryStatistics] {
    override def write(obj: LongSummaryStatistics): JsValue = JsObject(
      "count" -> JsNumber(obj.getCount),
      "max" -> JsNumber(obj.getMax),
      "min" -> JsNumber(obj.getMin),
      "average" -> JsNumber(obj.getAverage)
    )

    override def read(json: JsValue): LongSummaryStatistics = ???
  }

  implicit object StatsJsonFormat extends RootJsonFormat[Stats] {
    override def write(obj: Stats): JsValue = JsObject(
      "app" -> JsString(obj.app),
      "unit" -> JsString(obj.unit.name()),
      "startupDurations" -> JsArray(obj.startupDurations.map(JsNumber(_)).toVector),
      "summary" -> obj.summary.toJson
    )

    override def read(json: JsValue): Stats = ???
  }

}
