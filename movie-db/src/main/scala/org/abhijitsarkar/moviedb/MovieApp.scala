package org.abhijitsarkar.moviedb

import akka.http.scaladsl.Http
import reactivemongo.api.MongoDriver

/**
  * @author Abhijit Sarkar
  */
object MovieApp extends App with MovieController {
  override val repo = MongoDbMovieRepository(MongoDriver())
  override val client = OMDbClient()

  Http(system).bindAndHandle(routes, config.getString("http.host"), config.getInt("http.port"))
}
