package name.abhijitsarkar.user.repository

import com.typesafe.config.ConfigFactory
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

/**
  * @author Abhijit Sarkar
  */
trait DBContext {
  def dbName: String

  def configName: String

  // lazy because we need to wait for the subclasses to provide implementation for the above methods

  private lazy val config = ConfigFactory.load(configName)

  lazy val dbConfig = DatabaseConfig.forConfig[JdbcProfile](dbName, config)

  lazy val db = dbConfig.db
}
