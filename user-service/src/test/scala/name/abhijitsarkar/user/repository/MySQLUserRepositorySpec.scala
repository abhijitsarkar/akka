package name.abhijitsarkar.user.repository

import name.abhijitsarkar.user.TestUtil._
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.ExecutionContext.Implicits.global

class MySQLUserRepositorySpec extends UserRepositorySpec with BeforeAndAfterAll {

  val dBContext = new TestDBContext

  val dbConfig = dBContext.dbConfig
  val db = dBContext.db

  import dbConfig.driver.api._

  // Give the DB enough time to start up
  Thread.sleep(5000)

  override protected val userRepository = MySQLUserRepository(dBContext) {
    implicitly
  }

  val query = TableQuery[Users]

  override def afterAll() {
    println("Cleaning up")
    deleteAllUsers()
    db.close
  }

  override protected def dumpAllUsers = {
    println("Printing all users")
    db.run(query.result).map {
      println(_)
    }
  }

  override protected def deleteAllUsers() = {
    println("Deleting all users")
    db.run(query.delete)
  }

  override protected def someUserId = {
    randomUserId
  }
}