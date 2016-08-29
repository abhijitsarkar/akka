package name.abhijitsarkar.scauth

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import name.abhijitsarkar.scauth.model.OAuthCredentials
import name.abhijitsarkar.scauth.service.TwitterSearchService
import name.abhijitsarkar.scauth.util.ActorPlumbing

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

object TwitterApp extends App {
  implicit val system = ActorSystem("twitter")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = { implicitly }

  require(args.size == 2, "Usage: TwitterApp <consumerKey> <consumerSecret>")

  private val consumerKey = args(0).trim
  private val consumerSecret = args(1).trim

  val oAuthCredentials = OAuthCredentials(consumerKey, consumerSecret)
  implicit val actorPlumbing: ActorPlumbing = ActorPlumbing()

  val twitterService = new TwitterSearchService(oAuthCredentials)

  val searchResults = twitterService.search("@narendramodi")

  searchResults.onComplete {
    _ match {
      case Success(results) => println(results)
      case _ => println("Bad Twitter!")
    }
  }
}