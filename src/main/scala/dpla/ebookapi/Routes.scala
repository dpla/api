package dpla.ebookapi

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import dpla.ebookapi.v1.ebooks.{EbooksController, ElasticSearchClient, RawParams}

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
          parameters(
            "dataProvider".optional,
            "exact_field_match".optional,
            "facets".optional,
            "facet_size".optional,
            "isShownAt".optional,
            "object".optional,
            "page".optional,
            "page_size".optional,
            "q".optional,
            "sourceResource.creator".optional,
            "sourceResource.date.displayDate".optional,
            "sourceResource.description".optional,
            "sourceResource.format".optional,
            "sourceResource.language.name".optional,
            "sourceResource.publisher".optional,
            "sourceResource.subject.name".optional,
            "sourceResource.subtitle".optional,
            "sourceResource.title".optional) {

            (dataProvider,
             exactFieldMatch,
             facets,
             facetSize,
             isShownAt,
             `object`,
             page,
             pageSize,
             q,
             creator,
             date,
             description,
             format,
             language,
             publisher,
             subject,
             subtitle,
             title) =>

              val rawParams = RawParams(
                dataProvider = dataProvider,
                exactFieldMatch = exactFieldMatch,
                facets = facets,
                facetSize = facetSize,
                isShownAt = isShownAt,
                `object` = `object`,
                page = page,
                pageSize = pageSize,
                q = q,
                creator = creator,
                date = date,
                description = description,
                format = format,
                language = language,
                publisher = publisher,
                subject = subject,
                subtitle = subtitle,
                title = title
              )

              ebooksController.search(rawParams)
          }
        }
      },
      path(Segment) { id =>
        get {
          ebooksController.fetch(id)
        }
      }
    )

  lazy val healthCheckRoute: Route =
    get {
      complete(OK)
    }
}
