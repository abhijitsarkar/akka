package org.abhijitsarkar.moviedb

import org.scalatest.{FlatSpec, Matchers}

/**
  * @author Abhijit Sarkar
  */

import org.abhijitsarkar.moviedb.ExcelMovieParser._

class ExcelMovieParserSpec extends FlatSpec
  with Matchers {
  "ExcelMovieParser" should "extract name and year from file" in {
    val m = parseMovies(getClass.getResource("/test.xlsx"))

    m should have size 10
    m should contain(("Pretty Woman", "1990"))
    m should contain(("Aladdin", "1992"))
  }
}
