package dpla.publications

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import scala.concurrent.ExecutionContextExecutor
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.StatusCodes._
import akka.util.Timeout

import scala.util.{Failure, Success}

class PublicationRoutes(elasticSearchClient: ElasticSearchClient)(implicit val system: ActorSystem[_]) {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  // needed for the future map/onComplete
  implicit val executionContext: ExecutionContextExecutor = system.executionContext


  val publicationRoutes: Route =
    pathPrefix("publications") {
      concat(
        pathEnd {
          get {
            val params = SearchParams()
            onComplete(elasticSearchClient.search(params)) {
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
}
