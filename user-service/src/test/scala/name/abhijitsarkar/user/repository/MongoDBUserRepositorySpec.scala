package name.abhijitsarkar.user.repository

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.mongodb.casbah.commons.MongoDBObject
import de.flapdoodle.embed.mongo.MongodStarter
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder
import de.flapdoodle.embed.mongo.distribution.Version
import name.abhijitsarkar.user.repository.MongoDBCollectionFactory.newCollection
import org.bson.types.ObjectId
import org.scalatest.BeforeAndAfterAll

class MongoDBUserRepositorySpec extends UserRepositorySpec with BeforeAndAfterAll {
  implicit val system = ActorSystem("user-service")

  implicit def executor = system.dispatcher

  implicit val materializer = ActorMaterializer()

  val mongodConfig = new MongodConfigBuilder()
    .version(Version.Main.PRODUCTION)
    .build
  val starter = MongodStarter.getDefaultInstance

  val mongodExecutable = starter.prepare(mongodConfig)
  val mongod = mongodExecutable.start

  val host = mongodConfig.net().getServerAddress.getHostName
  val port = mongodConfig.net().getPort

  private val collection = newCollection("test", host, port)

  override protected val userRepository = MongoDBUserRepository(collection)(materializer)

  override def afterAll() {
    println("Cleaning up")
    collection.drop
    mongod.stop
    mongodExecutable.stop
  }

  override protected def dumpAllUsers = {
    println("Printing all users")
    collection.find().foreach { dbObj => println(dbObj.toMap) }
  }

  override protected def deleteAllUsers() = {
    collection.remove(MongoDBObject.empty)
  }

  override protected def someUserId = {
    new ObjectId().toString()
  }
}