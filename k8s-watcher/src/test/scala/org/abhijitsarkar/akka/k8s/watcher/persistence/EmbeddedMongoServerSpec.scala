package org.abhijitsarkar.akka.k8s.watcher.persistence

import de.flapdoodle.embed.process.runtime.Network
import org.scalatest.{FlatSpec, Matchers}

/**
  * @author Abhijit Sarkar
  */
class EmbeddedMongoServerSpec extends FlatSpec
  with Matchers {

  "EmbeddedMongoServer" should "return same PID if already running" in {
    val port = Network.getFreeServerPort()
    val pids = (1 to 3)
      .map(x => EmbeddedMongoServer.start(port = port))
      .map(_.getProcessId)
      .toSet

    EmbeddedMongoServer.stop("localhost", port)

    pids.size shouldBe (1)
  }
}
