package org.abhijitsarkar.moviedb

import java.io.File

import org.scalatest.{EitherValues, FlatSpec, Matchers}

/**
  * @author Abhijit Sarkar
  */
class MovieServiceSpec extends FlatSpec
  with Matchers
  with EitherValues
  with MovieService
  with MovieRepositoryHelper {
  "MovieService" should "extract name and year from file" in {
    val m = parseMovies(new File(getClass.getResource("/test.xlsx").toURI).getAbsolutePath)

    m should have size 10
    m should contain(("Pretty Woman", "1990"))
    m should contain(("Aladdin", "1992"))
  }
}
