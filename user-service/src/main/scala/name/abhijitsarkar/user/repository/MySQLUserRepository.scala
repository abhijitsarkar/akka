package name.abhijitsarkar.user.repository

import name.abhijitsarkar.user.domain.User
import org.slf4j.LoggerFactory

import scala.collection._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

// TODO: Compile queries
class MySQLUserRepository(private val dBContext: DBContext)(private implicit val executor: ExecutionContextExecutor) extends UserRepository {
  val logger = LoggerFactory.getLogger(getClass)

  val dbConfig = dBContext.dbConfig
  val db = dBContext.db

  import dbConfig.driver.api._

  val users = TableQuery[Users]

  def findByFirstName(firstName: String): Future[immutable.Seq[User]] = {
    val query = users.filter {
      _.firstName === firstName
    }

    run(query)
  }

  private def run(query: slick.lifted.Query[Users, User, Seq]): Future[immutable.Seq[User]] = {
    db.run(query.result).map {
      _.toList
    }
  }

  def findByLastName(lastName: String): Future[immutable.Seq[User]] = {
    val query = users.filter {
      _.lastName === lastName
    }

    run(query)
  }

  def findByFirstAndLastNames(firstName: String, lastName: String): Future[immutable.Seq[User]] = {
    val query = users.filter {
      _.firstName === firstName
    }.filter {
      _.lastName === lastName
    }

    run(query)
  }

  def findById(userId: String): Future[Option[User]] = {
    val query = users.filter {
      _.userId === userId
    }

    db.run(query.result.asTry).map {
      // Try[Seq[User]]
      case Success(users) => users match {
        // Seq[User]
        case immutable.Seq(user) =>
          logger.debug("Found user with user id: {}.", userId); Some(user)
        case _ => logger.warn("No user found with user id: {}.", userId); None
      }
      case Failure(ex) => logger.error(s"Failed to find user with user id: $userId", ex); None
    }
  }

  def updateUser(user: User) = {
    val userId = user.userId.get

    val q = (for {u <- users.filter {
      _.userId === userId
    }}
      yield (u.firstName, u.lastName, u.phoneNum, u.email))

    val action = q.update((user.firstName, user.lastName, user.phoneNum, user.email.getOrElse("")))

    logger.debug(s"Update statement: ${action.statements.head} for user id: ${user.userId}.")

    createUpdateOrDelete(user.userId.get, action)
  }

  private def createUpdateOrDelete(userId: String, action: DBIO[Int]) = {
    db.run(action.asTry).map {
      case Success(numUsersImpacted) if (numUsersImpacted > 0) => {
        logger.debug("Successfully created, updated or deleted user with user id: {}.", userId)
        Some(userId)
      }
      case Success(_) => None
      case Failure(ex) => logger.error(s"Failed to create, update or delete user with user id: $userId", ex); None
    }
  }

  def createUser(user: User) = {
    val action = users += user

    logger.debug(s"Insert statement: ${action.statements.head} for user id: ${user.userId}.")

    createUpdateOrDelete(user.userId.get, action)
  }

  def deleteUser(userId: String) = {
    val action = users.filter {
      _.userId === userId
    }.delete

    logger.debug(s"Delete statement: ${action.statements.head} for user id: ${userId}.")

    createUpdateOrDelete(userId, action)
  }
}

object MySQLUserRepository {
  def apply(dBContext: DBContext)(implicit executor: ExecutionContextExecutor) =
    new MySQLUserRepository(dBContext)(executor)
}