package name.abhijitsarkar.scauth.model

import java.time.ZonedDateTime

case class Tweet(text: Option[String], createdAt: ZonedDateTime, author: TwitterUser)