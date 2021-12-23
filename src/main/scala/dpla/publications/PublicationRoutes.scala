package dpla.publications

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

import scala.concurrent.{ExecutionContextExecutor, Future}
import dpla.publications.PublicationRegistry._
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.model.StatusCodes._
import akka.util.Timeout

import scala.util.{Failure, Success}

class PublicationRoutes(publicationRegistry: ActorRef[PublicationRegistry.Command])(implicit val system: ActorSystem[_]) {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  // needed for the future map/onComplete
  implicit val executionContext: ExecutionContextExecutor = system.executionContext

  def getPublications: Future[GetPublicationsResponse] =
    publicationRegistry.ask(GetPublications)
  def getPublication(id: String): Future[GetPublicationResponse] =
    publicationRegistry.ask(GetPublication(id, _))

  val publicationRoutes: Route =
    pathPrefix("publications") {
      concat(
        pathEnd {
          get {
            onComplete(ElasticSearchClient.all) {
              case Success(response) => response match {
                case Right(pubsFuture) =>
                  onComplete(pubsFuture) {
                    case Success(pubs) => complete(pubs)
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
              onComplete(ElasticSearchClient.find(id)) {
                case Success(response) => response match {
                  case Right(pub) => complete(pub)
                  case Left(status) => {
                    val code = status.value
                    val msg = status.reason
                    System.out.println(s"Error: $code: $msg")
                    complete(InternalServerError)
                  }
                }
                case Failure(e) => {
                  System.out.println(e)
                  complete(InternalServerError)
                }
              }
            }
          }
        }
      )
    }
}
