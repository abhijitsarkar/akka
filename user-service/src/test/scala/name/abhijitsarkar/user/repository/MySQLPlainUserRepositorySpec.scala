package name.abhijitsarkar.user.repository

import name.abhijitsarkar.user.TestUtil._
import name.abhijitsarkar.user.domain.User
import org.scalatest.BeforeAndAfterAll
import slick.jdbc.GetResult

import scala.concurrent.ExecutionContext.Implicits.global

class MySQLPlainUserRepositorySpec extends UserRepositorySpec with BeforeAndAfterAll {

  val dBContext = new TestDBContext

  val dbConfig = dBContext.dbConfig
  val db = dBContext.db

  import dbConfig.driver.api._

  // Give the DB enough time to start up
  Thread.sleep(5000)

  override protected val userRepository = MySQLPlainUserRepository(dBContext) {
    implicitly
  }

  val table = "users"
  implicit val getUserResult = GetResult[User](u => User(u.<<, u.<<, u.<<, u.<<, u.<<))

  override def afterAll() {
    println("Cleaning up")
    deleteAllUsers()
    db.close
  }

  override protected def dumpAllUsers = {
    println("Printing all users")
    val query = sql"SELECT * FROM #$table".as[User]

    db.run(query).map {
      println(_)
    }
  }

  override protected def deleteAllUsers() = {
    println("Deleting all users")
    val action = sqlu"DELETE FROM #$table"

    db.run(action)
  }

  override protected def someUserId = {
    randomUserId
  }
}