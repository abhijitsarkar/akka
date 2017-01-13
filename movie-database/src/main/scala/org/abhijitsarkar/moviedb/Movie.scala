package org.abhijitsarkar.moviedb

import spray.json.{DefaultJsonProtocol, DeserializationException, JsObject, JsString, JsValue, RootJsonFormat}

import scala.util.Try

/**
  * @author Abhijit Sarkar
  */
case class Movie(title: String, year: Int, imdbRating: Double, imdbId: String)

object MovieProtocol extends DefaultJsonProtocol {

  implicit object ColorJsonFormat extends RootJsonFormat[Movie] {
    def write(m: Movie) = JsObject(
      "title" -> JsString(m.title),
      "year" -> JsString(m.year.toString),
      "imdbRating" -> JsString(m.imdbRating.toString),
      "imdbId" -> JsString(m.imdbId)
    )

    def read(value: JsValue) = {
      value.asJsObject.getFields("Title", "Year", "imdbRating", "imdbID") match {
        case Seq(JsString(title), JsString(year), JsString(imdbRating), JsString(imdbId)) =>
          new Movie(title, Try(year.toInt).getOrElse(-1), Try(imdbRating.toDouble).getOrElse(-1.0), imdbId)
        case _ => throw new DeserializationException("Failed to unmarshal Movie.")
      }
    }
  }

}
