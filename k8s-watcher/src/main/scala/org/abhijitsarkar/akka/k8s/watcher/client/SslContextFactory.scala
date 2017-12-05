package org.abhijitsarkar.akka.k8s.watcher.client

import java.io.FileInputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.{SSLContext, TrustManagerFactory}

import akka.actor.ActorSystem
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}

/**
  * @author Abhijit Sarkar
  */
object SslContextFactory {
  def createClientSslContext(certFile: String)(implicit system: ActorSystem) = {
    val cf: CertificateFactory = CertificateFactory.getInstance("X.509")
    val is = new FileInputStream(certFile)
    val cacert = cf.generateCertificate(is)
    is.close()

    val ks: KeyStore = KeyStore.getInstance(KeyStore.getDefaultType)
    ks.load(null) // we don't need the KeyStore instance to come from a file.
    ks.setCertificateEntry("cacert", cacert)

    val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
    tmf.init(ks)
    val sslContext: SSLContext = SSLContext.getInstance("TLS")
    sslContext.init(null, tmf.getTrustManagers, null)
    val https: HttpsConnectionContext = ConnectionContext.https(sslContext)

    val http = Http()
    http.setDefaultClientHttpsContext(https)

    http
  }
}
