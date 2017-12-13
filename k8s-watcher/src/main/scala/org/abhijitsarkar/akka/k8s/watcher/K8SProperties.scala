package org.abhijitsarkar.akka.k8s.watcher

import java.net.URI

/**
  * @author Abhijit Sarkar
  */
case class K8SProperties(
                          baseUrl: String = "http://localhost:9000",
                          namespace: String = "default",
                          apiTokenFile: Option[String] = None,
                          apiToken: Option[String] = None,
                          apps: List[String] = Nil,
                          deletionEnabled: Boolean = true,
                          deletionInitialDelayMin: Long = 1L,
                          deletionIntervalMin: Long = 2L
                        ) {
  private val u = URI.create(baseUrl)

  val host = u.getHost
  val port: Int = u.getPort match {
    case -1 => if (u.getScheme.startsWith("https")) 443 else 80
    case x => x
  }
}
