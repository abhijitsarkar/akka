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
    m should contain(("Final Fantasy", "2001"))
    m should contain(("Aladdin", "1992"))
  }

  "ExcelMovieParser" should "take until last -" in {
    val title = takeUntil(takeUntil("Universal Soldier - CD 2", '.'), '-').trim

    title shouldBe("Universal Soldier")
  }
}
