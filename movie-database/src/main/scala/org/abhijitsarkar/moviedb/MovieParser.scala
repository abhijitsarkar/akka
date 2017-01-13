package org.abhijitsarkar.moviedb

import java.net.URL

/**
  * @author Abhijit Sarkar
  */
trait MovieParser {
  def parseMovies(url: URL): Seq[(String, String)]
}
