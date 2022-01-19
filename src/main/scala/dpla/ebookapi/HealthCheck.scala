package dpla.ebookapi

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout

import scala.concurrent.ExecutionContextExecutor

class PublicationRoutes()(implicit val system: ActorSystem[_]) {

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  // needed for the future map/onComplete
  implicit val executionContext: ExecutionContextExecutor = system.executionContext

  val publicationRoutes: Route = {
    pathPrefix("health-check") {
      concat(
        pathEnd {
          get {
            complete(OK)
          }
        }
      )
    }
  }
}
