package org.abhijitsarkar.akka.k8s.watcher.repository

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}

import akka.actor.{Actor, ActorLogging}
import akka.pattern.pipe
import org.abhijitsarkar.akka.k8s.watcher.ActorModule
import org.abhijitsarkar.akka.k8s.watcher.domain._
import reactivemongo.api.Cursor.ContOnError
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONReader, BSONString, BSONValue, BSONWriter, Macros, _}

import scala.concurrent.Future

/**
  * @author Abhijit Sarkar
  */
trait Repository {
  implicit val eventTypeWriter = new EnumBsonWriter(EventType)
  implicit val eventTypeReader = new EnumBsonReader(EventType)
  implicit val podPhaseWriter = new EnumBsonWriter(PodPhase)
  implicit val podPhaseReader = new EnumBsonReader(PodPhase)
  implicit val podConditionStatusTypeWriter = new EnumBsonWriter(PodConditionStatusType)
  implicit val podConditionStatusTypeReader = new EnumBsonReader(PodConditionStatusType)
  implicit val podConditionStatusWriter = new EnumBsonWriter(PodConditionStatus)
  implicit val podConditionStatusReader = new EnumBsonReader(PodConditionStatus)

  implicit val instantReader = new BSONReader[BSONValue, Instant] {
    override def read(bson: BSONValue) = bson match {
      case BSONString(txt) => Instant.parse(txt)
      case unknown => throw new RuntimeException(s"Failed to deserialize instant, got $unknown.")
    }
  }

  implicit val instantWriter = new BSONWriter[Instant, BSONString] {
    override def write(obj: Instant) = BSONString(DateTimeFormatter.ISO_INSTANT
      .withZone(ZoneId.of("UTC"))
      .format(obj))
  }

  implicit val podConditionReader: BSONDocumentReader[PodCondition] = Macros.reader[PodCondition]
  implicit val podConditionWriter: BSONDocumentWriter[PodCondition] = Macros.writer[PodCondition]

  implicit val podStatusReader: BSONDocumentReader[PodStatus] = Macros.reader[PodStatus]
  implicit val podStatusWriter: BSONDocumentWriter[PodStatus] = Macros.writer[PodStatus]

  implicit val metadataReader: BSONDocumentReader[Metadata] = Macros.reader[Metadata]
  implicit val metadataWriter: BSONDocumentWriter[Metadata] = Macros.writer[Metadata]

  implicit val podReader: BSONDocumentReader[Pod] = Macros.reader[Pod]
  implicit val podWriter: BSONDocumentWriter[Pod] = Macros.writer[Pod]

  implicit val eventReader: BSONDocumentReader[Event] = Macros.reader[Event]
  implicit val eventWriter: BSONDocumentWriter[Event] = Macros.writer[Event]

  class EnumBsonWriter[T <: scala.Enumeration](enu: T) extends BSONWriter[T#Value, BSONString] {
    override def write(t: T#Value) = BSONString(t.toString)
  }

  class EnumBsonReader[T <: scala.Enumeration](enu: T) extends BSONReader[BSONValue, T#Value] {
    override def read(bson: BSONValue) = bson match {
      case BSONString(txt) => enu.withName(txt)
      case unknown => throw new RuntimeException(s"Can't deserialize $enu from $unknown.")
    }
  }

}

class MongoRepositoryActor(
                            eventCollection: Future[BSONCollection],
                            actorModule: ActorModule
                          ) extends Actor with ActorLogging with Repository {

  import actorModule._

  override def receive = {
    case PersistEventRequest(event, replyTo) => {
      val fn = (n: Int) => (x: Option[String]) => x match {
        case Some(x) => Left(x)
        case _ => Right(n)
      }

      val pod = event.`object`
      val id = pod.uid
      val app = pod.app.getOrElse("UNKNOWN")
      val doc = eventWriter.write(event)

      val result = (for {
        coll <- eventCollection
        x <- coll.update(selector = BSONDocument("_id" -> id), update = doc, upsert = true)
          .map(result => (result.n, result.errmsg))
      } yield x)
        .map(x => (app, fn(x._1)(x._2)))

      result.map(PersistEventResponse).pipeTo(replyTo)
    }
    //    case PersistEventsRequest(events) => {
    //      val fn = (n: Int) => (x: Option[String]) => x match {
    //        case Some(x) => log.error(x); Left(x)
    //        case _ => Right(n)
    //      }
    //
    //      val flow: Flow[EitherT[Future, String, Event], (String, Either[String, Int]), NotUsed] =
    //        Flow[EitherT[Future, String, Event]]
    //          .mapAsyncUnordered(Runtime.getRuntime.availableProcessors()) { elem =>
    //            elem.semiflatMap { event =>
    //              val id = event.`object`.metadata.uid
    //
    //              for {
    //                coll <- eventCollection
    //                x <- coll.update(selector = BSONDocument("_id" -> id), update = eventWriter.write(event), upsert = true)
    //                  .map(result => (event, result.n, result.errmsg))
    //              } yield x
    //            }
    //              .map { x =>
    //                x._1.`object`.metadata.labels.get("app")
    //                  .orElse(Some("UNKNOWN"))
    //                  .map((_, fn(x._2)(x._3)))
    //                  .get
    //              }
    //              .fold(x => ("UNKNOWN", Left(x)), identity)
    //          }
    //
    //      serviceActor ! PersistEventsResponse(events.via(flow))
    //    }
    case FindByPodUidRequest(uid, replyTo)
    => {
      val x = for {
        coll <- eventCollection
        event <- coll.find(BSONDocument("_id" -> uid)).one[Event]
      } yield event

      x.map(FindByPodUidResponse).pipeTo(replyTo)
    }

    case FindByAppRequest(app, replyTo, uuid)
    => {
      val x = for {
        coll <- eventCollection
        event <- coll.find(BSONDocument("object.metadata.labels.app" -> app))
          .cursor[Event]()
          .collect(25, ContOnError[List[Event]]((e, t) => log.error(t, s"Failed to find event: $e")))
      } yield event

      x.map(y => FindByAppResponse(y, uuid)).pipeTo(replyTo)
    }
  }
}


