package name.abhijitsarkar.scauth

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Sink}
import akka.stream.{ActorMaterializer, SinkShape}
import akka.util.ByteString
import name.abhijitsarkar.scauth.model.TwitterJsonSupport.parseTweet
import name.abhijitsarkar.scauth.model.{OAuthCredentials, Tweet}
import name.abhijitsarkar.scauth.service.{TwitterStreamingService, TwitterSubscriber}
import name.abhijitsarkar.scauth.util.ActorPlumbing

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

object TwitterStreamingApp extends App {
  implicit val system = ActorSystem("twitter")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = {
    implicitly
  }

  require(args.size == 4, "Usage: YelpApp <consumerKey> <consumerSecret> <token> <tokenSecret>")

  private val consumerKey = args(0).trim
  private val consumerSecret = args(1).trim
  private val token = args(2).trim
  private val tokenSecret = args(3).trim

  val oAuthCredentials = OAuthCredentials(consumerKey, consumerSecret, Some(token), Some(tokenSecret))
  implicit val actorPlumbing: ActorPlumbing = ActorPlumbing()

  val beforeEpochSubscriber = Sink.actorSubscriber(TwitterSubscriber.props("BeforeEpoch"))
  val afterEpochSubscriber = Sink.actorSubscriber(TwitterSubscriber.props("AfterEpoch"))

  val tweetsFlow = Flow[ByteString].map {
    b => parseTweet(b.utf8String)
  }

  val isAfterEpoch = (t: Tweet) => t.createdAt.getYear > 1970

  val beforeEpochTweets = Flow[Tweet].filter {
    isAfterEpoch
  }

  // The negation of isAfterEpoch is a function that applies isAfterEpoch to its argument and negates the result.
  val afterEpochTweets = Flow[Tweet].filter {
    !isAfterEpoch(_)
  }

  // Good read: https://github.com/akka/akka/issues/18505
  // val pub = Sink.fanoutPublisher[Tweet](1, 1)

  val partial = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val broadcast = builder.add(Broadcast[Tweet](2))

    val tweets = builder.add(tweetsFlow)

    broadcast ~> beforeEpochTweets ~> beforeEpochSubscriber
    broadcast ~> afterEpochTweets ~> afterEpochSubscriber

    tweets ~> broadcast

    SinkShape(tweets.in)
  }

  val twitterService = new TwitterStreamingService[Tweet](oAuthCredentials, partial)

  twitterService.stream(None, Some("narendramodi"))
}