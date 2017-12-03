package org.abhijitsarkar.akka.k8s.watcher.repository

/**
  * @author Abhijit Sarkar
  */

import java.util.concurrent.{ConcurrentHashMap => JavaConcurrentMap}

import de.flapdoodle.embed.mongo.config.{IMongodConfig, MongodConfigBuilder, Net}
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.mongo.{MongodExecutable, MongodProcess, MongodStarter}
import de.flapdoodle.embed.process.runtime.Network
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.collection.concurrent.{Map => ConcurrentMap}


object EmbeddedMongoServer {

  case class Address(host: String, port: Int)

  val DefaultAddress = Address("localhost", 27017)

  val starter: MongodStarter = MongodStarter.getDefaultInstance
  val log = LoggerFactory.getLogger(EmbeddedMongoServer.getClass)

  val instanceMap: ConcurrentMap[Address, (MongodExecutable, MongodProcess)] =
    (new JavaConcurrentMap[Address, (MongodExecutable, MongodProcess)]()).asScala

  def start(host: String = DefaultAddress.host, port: Int = DefaultAddress.port): MongodProcess = {
    val addr = Address(host, port)
    instanceMap.getOrElseUpdate(addr, {
      val mongodExecutable = starter.prepare(mongodConfig(host, port))
      val mongod = mongodExecutable.start()
      logProcessInfo(mongod)

      (mongodExecutable, mongod)
    })
      ._2
  }

  private def mongodConfig(host: String, port: Int): IMongodConfig = new MongodConfigBuilder()
    .version(Version.Main.PRODUCTION)
    .net(new Net(host, port, Network.localhostIsIPv6))
    .build

  private def logProcessInfo(process: MongodProcess): MongodProcess = {
    val net = process.getConfig.net()

    log.info("Embedded MongoDB server started on: {}:{}.", net.getBindIp, net.getPort)

    process
  }

  def stop(host: String = DefaultAddress.host, port: Int = DefaultAddress.port): Unit = {
    instanceMap.remove(Address(host, port))
      .foreach { addr =>
        addr._1.stop()
        addr._2.stop()
      }
  }
}
