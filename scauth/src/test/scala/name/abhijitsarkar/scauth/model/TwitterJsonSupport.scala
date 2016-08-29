package name.abhijitsarkar.scauth.model

import java.time.format.DateTimeFormatter
import java.time.{ZoneId, ZonedDateTime}

import org.slf4j.LoggerFactory
import spray.json.{DefaultJsonProtocol, _}

import scala.util.{Failure, Success, Try}

object TwitterJsonSupport extends DefaultJsonProtocol {
  private val log = LoggerFactory.getLogger(getClass())

  implicit object DateJsonFormat extends RootJsonFormat[ZonedDateTime] {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(""""EEE MMM dd HH:mm:ss xxxx yyyy"""")

    override def write(obj: ZonedDateTime) = JsString(obj.format(formatter))

    override def read(json: JsValue): ZonedDateTime = json match {
      case JsString(s) => ZonedDateTime.parse(json.toString, formatter)
      case _ => throw new DeserializationException(s"Failed to parse date time: $json.")
    }
  }

  implicit val authorFormat = jsonFormat(TwitterUser, "name", "screen_name", "location", "followers_count")
  implicit val tweetFormat = jsonFormat(Tweet, "text", "created_at", "user")

  private val empty = ""
  private val unknownUser = TwitterUser(empty, empty, empty, -1)
  val epoch = ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
  private val emptyTweet = Tweet(None, epoch, unknownUser)

  def parseTweet(str: String): Tweet = {
    Try(str.parseJson.convertTo[Tweet]) match {
      case Success(tweet) => log.debug("Successfully parsed tweet: {}.", str); tweet
      case Failure(ex) => log.error(s"Failed to parse tweet: {}.", ex.getMessage); emptyTweet.copy(text = Some(str))
    }
  }
}