package org.abhijitsarkar.moviedb

import java.text.NumberFormat.getNumberInstance
import java.util.Locale.US

import spray.json.{DefaultJsonProtocol, DeserializationException, JsArray, JsObject, JsString, JsValue, RootJsonFormat}

import scala.util.Try

/**
  * @author Abhijit Sarkar
  */
case class Movie(
                  title: String,
                  year: Int,
                  genres: Seq[String],
                  mpaaRating: String,
                  runtime: String,
                  directors: Seq[String],
                  actors: Seq[String],
                  language: String,
                  country: String,
                  `type`: String,
                  plot: String,
                  imdbVotes: Long,
                  imdbRating: Double,
                  imdbId: String
                )

object MovieProtocol extends DefaultJsonProtocol {

  implicit object ColorJsonFormat extends RootJsonFormat[Movie] {
    def write(m: Movie) = JsObject(
      "title" -> JsString(m.title),
      "year" -> JsString(m.year.toString),
      "genres" -> JsArray(m.genres.map(JsString(_)).toVector),
      "mpaaRating" -> JsString(m.mpaaRating),
      "runtime" -> JsString(m.runtime),
      "directors" -> JsArray(m.directors.map(JsString(_)).toVector),
      "actors" -> JsArray(m.actors.map(JsString(_)).toVector),
      "language" -> JsString(m.language),
      "country" -> JsString(m.country),
      "type" -> JsString(m.`type`),
      "plot" -> JsString(m.plot),
      "imdbVotes" -> JsString(m.imdbVotes.toString),
      "imdbRating" -> JsString(m.imdbRating.toString),
      "imdbId" -> JsString(m.imdbId)
    )

    def read(value: JsValue) = {
      value.asJsObject.getFields("Title", "Year", "Genre", "Rated", "Runtime", "Director", "Actors", "Language",
        "Country", "Type", "Plot", "imdbVotes", "imdbRating", "imdbID") match {
        case Seq(
        JsString(title),
        JsString(year),
        JsString(genres),
        JsString(mpaaRating),
        JsString(runtime),
        JsString(directors),
        JsString(actors),
        JsString(language),
        JsString(country),
        JsString(typ),
        JsString(plot),
        JsString(imdbVotes),
        JsString(imdbRating),
        JsString(imdbId)
        ) => new Movie(
          title,
          Try(year.toInt).getOrElse(-1),
          genres.split(","),
          mpaaRating,
          runtime,
          directors.split(","),
          actors.split(","),
          language,
          country,
          typ,
          plot,
          Try(getNumberInstance(US).parse(imdbVotes).longValue).getOrElse(-1),
          Try(imdbRating.toDouble).getOrElse(-1.0),
          imdbId
        )
        case _ => throw new DeserializationException("Failed to unmarshal Movie.")
      }
    }
  }

}
