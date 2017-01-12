package org.abhijitsarkar.moviedb

import spray.json.{DefaultJsonProtocol, DeserializationException, JsObject, JsString, JsValue, RootJsonFormat}

/**
  * @author Abhijit Sarkar
  */
case class Movie(title: String, year: String, imdbRating: String, imdbId: String)

object MovieProtocol extends DefaultJsonProtocol {

  implicit object ColorJsonFormat extends RootJsonFormat[Movie] {
    def write(m: Movie) = JsObject(
      "title" -> JsString(m.title),
      "year" -> JsString(m.year),
      "imdbRating" -> JsString(m.imdbRating),
      "imdbId" -> JsString(m.imdbId)
    )

    def read(value: JsValue) = {
      value.asJsObject.getFields("Title", "Year", "imdbRating", "imdbID") match {
        case Seq(JsString(title), JsString(year), JsString(imdbRating), JsString(imdbId)) =>
          new Movie(title, year, imdbRating, imdbId)
        case _ => throw new DeserializationException("Failed to unmarshall Movie.")
      }
    }
  }

}
