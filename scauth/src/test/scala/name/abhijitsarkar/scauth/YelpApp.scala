package name.abhijitsarkar.scauth

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import name.abhijitsarkar.scauth.model.OAuthCredentials
import name.abhijitsarkar.scauth.service.YelpService
import name.abhijitsarkar.scauth.util.ActorPlumbing

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

object YelpApp extends App {
  implicit val system = ActorSystem("yelp")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = { implicitly }

  require(args.size == 4, "Usage: YelpApp <consumerKey> <consumerSecret> <token> <tokenSecret>")

  private val consumerKey = args(0).trim
  private val consumerSecret = args(1).trim
  private val token = args(2).trim
  private val tokenSecret = args(3).trim

  val oAuthCredentials = OAuthCredentials(consumerKey, consumerSecret, Some(token), Some(tokenSecret))
  implicit val actorPlumbing: ActorPlumbing = ActorPlumbing()

  val yelpService = new YelpService(oAuthCredentials)

  val searchResults = yelpService.searchForBusinessesByLocation("dinner", "San Francisco, CA")

  searchResults.onComplete {
    _ match {
      case Success(results) => println(results)
      case _ => println("Bad Yelp!")
    }
  }
}