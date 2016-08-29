package name.abhijitsarkar.akka.model

import spray.json.DefaultJsonProtocol

object RsvpJsonSupport extends DefaultJsonProtocol {
  implicit val venueFormat = jsonFormat(Venue, "venue_name")
  implicit val eventFormat = jsonFormat(Event, "event_name")
  implicit val memberFormat = jsonFormat(Member, "member_name")

  implicit val rsvpFormat = jsonFormat3(Rsvp)
}