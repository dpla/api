package dpla.ebookapi

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.util.Timeout
import dpla.ebookapi.v1.ebooks.{ElasticSearchClient, ParamValidator, RawParams, ValidationException}
import dpla.ebookapi.v1.ebooks.JsonFormats._

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

class Routes(elasticSearchClient: ElasticSearchClient)(implicit val system: ActorSystem[_]) {

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  // needed for the future map/onComplete
  implicit val executionContext: ExecutionContextExecutor = system.executionContext

  val applicationRoutes: Route = {
    concat(
      pathPrefix("v1") {
        concat(
          pathPrefix("ebooks") {
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

                      ParamValidator.getSearchParams(rawParams) match {
                        case Success(validParams) =>
                          onComplete(elasticSearchClient.search(validParams)) {
                            case Success(response) => response match {
                              case Right(pubsFuture) =>
                                onComplete(pubsFuture) {
                                  case Success(pubs) =>
                                    complete(pubs)
                                  case Failure(e) =>
                                    // Failure to parse ElasticSearch response
                                    System.out.println(s"Error: $e")
                                    complete(InternalServerError)
                                }
                              case Left(status) =>
                                // ElasticSearch returned an error
                                val code = status.value
                                val msg = status.reason
                                System.out.println(s"Error: $code: $msg")
                                complete(InternalServerError)
                            }
                            case Failure(e) =>
                              // The call to the ElasticSearch API failed
                              System.out.println(e)
                              complete(InternalServerError)
                          }
                        case Failure(e: ValidationException) =>
                          // The user submitted invalid parameters
                          System.out.println(e.getMessage)
                          complete(HttpResponse(BadRequest, entity = e.getMessage))
                        case Failure(e) =>
                          // This shouldn't happen
                          System.out.println(e.getMessage)
                          complete(HttpResponse(InternalServerError, entity = e.getMessage))
                      }
                  }
                }
              },
              path(Segment) { id =>
                get {
                  rejectEmptyResponse {

                    onComplete(elasticSearchClient.fetch(id)) {
                      case Success(response) => response match {
                        case Right(pubFuture) =>
                          onComplete(pubFuture) {
                            case Success(pub) =>
                              complete(pub)
                            case Failure(e) =>
                              // Failure to parse ElasticSearch response
                              System.out.println(s"Error: $e")
                              complete(InternalServerError)
                          }
                        case Left(status) =>
                          // ElasticSearch returned an error
                          val code = status.value
                          val msg = status.reason
                          System.out.println(s"Error: $code: $msg")
                          complete(InternalServerError)
                      }
                      case Failure(e) =>
                        // The call to the ElasticSearch API failed
                        System.out.println(e)
                        complete(InternalServerError)
                    }
                  }
                }
              }
            )
          }
        )
      },
      pathPrefix("health-check") {
        concat(
          pathEnd {
            get {
              complete(OK)
            }
          }
        )
      }
    )
  }
}
