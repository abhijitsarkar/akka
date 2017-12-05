package org.abhijitsarkar.akka.k8s.watcher

import java.net.URL

/**
  * @author Abhijit Sarkar
  */
case class K8SProperties(
                          baseUrl: String = "http://localhost:9000",
                          namespace: String = "default",
                          certFile: Option[String] = None,
                          apiTokenFile: Option[String] = None,
                          apiToken: Option[String] = None,
                          apps: List[String] = Nil
                        ) {
  private val u = new URL(baseUrl)

  val host = u.getHost
  val port = if (u.getPort == -1) {
    if (u.getProtocol == "https") 443 else if (u.getProtocol == "http") 80 else u.getPort
  }
}
