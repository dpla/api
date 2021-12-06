package dpla.publications

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

import scala.concurrent.Future
import dpla.publications.PublicationRegistry._
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout

class PublicationRoutes(publicationRegistry: ActorRef[PublicationRegistry.Command])(implicit val system: ActorSystem[_]) {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  def getPublications: Future[Publications] =
    publicationRegistry.ask(GetPublications)
  def getPublication(id: String): Future[GetPublicationResponse] =
    publicationRegistry.ask(GetPublication(id, _))

  val publicationRoutes: Route =
    pathPrefix("publications") {
      concat(
        pathEnd {
          get {
            complete(getPublications)
          }
        },
        path(Segment) { id =>
          get {
            rejectEmptyResponse {
              onSuccess(getPublication(id)) { response =>
                complete(response.maybePublication)
              }
            }
          }
        }
      )
    }
}
