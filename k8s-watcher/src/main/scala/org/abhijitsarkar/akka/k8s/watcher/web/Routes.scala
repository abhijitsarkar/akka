package org.abhijitsarkar.akka.k8s.watcher.web

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.softwaremill.tagging.@@
import org.abhijitsarkar.akka.k8s.watcher.domain.{GetAppsRequest, GetStatsForOneRequest, GetStatsRequest}
import org.abhijitsarkar.akka.k8s.watcher.model.Stats
import org.abhijitsarkar.akka.k8s.watcher.model.StatsJsonProtocol._

/**
  * @author Abhijit Sarkar
  */
//val routes: Route = pathPrefix("foo") {
//get {
//complete("GET /foo")
//} ~
//pathPrefix(Segment) { seg =>
//get {
//complete(s"GET /foo/$seg")
//}
//}
//}
//The ~ operator stops at the first operand, since all the condition are met (starts with /foo and is a GET).
//You might want to pass the more precise one as first operand, or specify that you want a PathEnd in your first case.
trait Routes {
  val route: @@[ActorRef, Web] => Route = (requestHandler: ActorRef @@ Web) => {
    get {
      pathPrefix("apps") {
        pathEndOrSingleSlash {
          logRequest("/apps") {
            completeWith(instanceOf[List[String]]) { callback =>
              requestHandler ! GetAppsRequest(callback)
            }
          }
        } ~ path("stats") {
          logRequest("/stats") {
            completeWith(instanceOf[List[Stats]]) { callback =>
              requestHandler ! GetStatsRequest(callback)
            }
          }
        }
      } ~ path("apps" / Segment / "stats") { app =>
        logRequest(s"/apps/$app/stats") {
          completeWith(instanceOf[Stats]) { callback =>
            requestHandler ! GetStatsForOneRequest(app, callback)
          }
        }
      }
    }
  }
}
