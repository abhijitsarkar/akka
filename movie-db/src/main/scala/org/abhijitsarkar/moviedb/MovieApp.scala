package org.abhijitsarkar.moviedb

import akka.http.scaladsl.Http

/**
  * @author Abhijit Sarkar
  */
object MovieApp extends App with MovieController {
  Http(system).bindAndHandle(routes, config.getString("http.host"), config.getInt("http.port"))
}
