package dpla.ebookapi

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import dpla.ebookapi.v1.ebooks.{EbooksController, ElasticSearchClient}

class Routes(elasticSearchClient: ElasticSearchClient)(implicit val system: ActorSystem[_]) {

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  val ebooksController = new EbooksController(elasticSearchClient)

  lazy val applicationRoutes: Route =
    concat (
      pathPrefix("ebooks")(ebooksRoutes),
      pathPrefix("v1") {
        concat(
          pathPrefix("ebooks")(ebooksRoutes)
        )
      },
      path("health-check")(healthCheckRoute)
    )

  lazy val ebooksRoutes: Route =
    concat(
      pathEnd {
        get {
          parameterMap { params =>
            ebooksController.search(params)
          }
        }
      },
      path(Segment) { id =>
        get {
          parameterMap { params =>
            ebooksController.fetch(id, params)
          }
        }
      }
    )

  lazy val healthCheckRoute: Route =
    get {
      complete(OK)
    }
}
