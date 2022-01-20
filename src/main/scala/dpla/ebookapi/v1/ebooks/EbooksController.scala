package dpla.ebookapi.v1.ebooks

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes.{BadRequest, InternalServerError}
import akka.http.scaladsl.server.Directives.{complete, onComplete}

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Route
import dpla.ebookapi.v1.ebooks.JsonFormats._

class EbooksController(elasticSearchClient: ElasticSearchClient)(implicit val system: ActorSystem[_]) {

  // needed for the future map/onComplete
  implicit val executionContext: ExecutionContextExecutor = system.executionContext

  def search(rawParams: RawParams): Route = {
    ParamValidator.getSearchParams(rawParams) match {
      case Success(validParams) =>
        onComplete(elasticSearchClient.search(validParams)) {
          case Success(response) => response match {
            case Right(ebooksFuture) =>
              onComplete(ebooksFuture) {
                case Success(ebooks) =>
                  // Success
                  complete(ebooks)
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

  def fetch(id: String): Route = {
    ParamValidator.getValidId(id) match {
      case Success(validId) =>
        onComplete (elasticSearchClient.fetch (validId) ) {
          case Success (response) => response match {
            case Right (ebookFuture) =>
              onComplete (ebookFuture) {
                case Success (ebook) =>
                  //Success
                  complete (ebook)
                case Failure (e) =>
                  // Failure to parse ElasticSearch response
                  System.out.println (s"Error: $e")
                  complete (InternalServerError)
              }
            case Left (status) =>
              // ElasticSearch returned an error
              val code = status.value
              val msg = status.reason
              System.out.println (s"Error: $code: $msg")
              complete (InternalServerError)
            }
          case Failure (e) =>
            // The call to the ElasticSearch API failed
            System.out.println(e)
            complete (InternalServerError)
        }
      case Failure(e) =>
        // User submitted an invalid ID
        System.out.println (e)
        complete(BadRequest, e.getMessage)
    }
  }
}
