package name.abhijitsarkar.akka

import akka.actor.{ActorLogging, Props}
import akka.stream.actor.ActorSubscriberMessage.{OnComplete, OnError, OnNext}
import akka.stream.actor.{ActorSubscriber, WatermarkRequestStrategy}

class FibonacciSubscriber(name: String) extends ActorSubscriber with ActorLogging {
  /**
    * If the number of unhandled messages is less than the low watermark (default is half of high watermark),
    * this strategy requests enough elements to meet the high watermark.
    */
  val requestStrategy = WatermarkRequestStrategy(20)

  def receive = {
    case OnNext(fib: Long) =>
      log.debug("Received Fibonacci number: {}.", fib)

      if (fib > 5000) self ! OnComplete
    case OnError(ex: Exception) =>
      log.error(ex.getMessage, ex)
      self ! OnComplete
    case OnComplete =>
      log.info("Stopping subscriber.")
      context.stop(self)
    case unknown => log.warning("Received unknown event: ${}.", unknown)
  }
}

object FibonacciSubscriber {
  def props(name: String) = Props(new FibonacciSubscriber(name))
}