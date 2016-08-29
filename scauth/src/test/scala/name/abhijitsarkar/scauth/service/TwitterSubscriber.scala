package name.abhijitsarkar.scauth.service

import akka.actor.{Props, actorRef2Scala}
import akka.stream.actor.ActorSubscriberMessage.{OnComplete, OnError, OnNext}
import akka.stream.actor.{ActorSubscriber, WatermarkRequestStrategy}
import name.abhijitsarkar.scauth.model.Tweet
import org.slf4j.LoggerFactory

class TwitterSubscriber(name: String) extends ActorSubscriber {
  private val log = LoggerFactory.getLogger(name)

  val requestStrategy = WatermarkRequestStrategy(10)

  def receive = {
    case OnNext(tweet: Tweet) =>
      log.debug("Received tweet: {}.", tweet)
    case OnError(ex: Exception) =>
      log.error(ex.getMessage, ex)
      self ! OnComplete
    case OnComplete =>
      log.info(s"$name completed.")
      context.stop(self)
    case unknown => log.warn("Unknown event: {}.", unknown)
  }
}

object TwitterSubscriber {
  def props(name: String) = Props(new TwitterSubscriber(name))
}