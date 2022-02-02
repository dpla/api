package dpla.ebookapi

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import dpla.ebookapi.v1.ebooks.EbookRegistry.{Fetch, Search}
import dpla.ebookapi.v1.ebooks.{EbookRegistry, FetchResult, InternalFailure, NotFoundFailure, SearchResult, ValidationFailure}

import scala.concurrent.{ExecutionContextExecutor, Future}
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.headers.RawHeader

import scala.util.{Failure, Success}
import dpla.ebookapi.v1.ebooks.JsonFormats._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._


class Routes(ebookRegistry: ActorRef[EbookRegistry.Command])(implicit val system: ActorSystem[_]) {

  // needed for the future map/onComplete
  implicit val executionContext: ExecutionContextExecutor = system.executionContext

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout: Timeout =
    Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  // TODO should I not use ask here?
  def searchEbooks(params: Map[String, String]): Future[SearchResult] =
    ebookRegistry.ask(Search(params, _))

  def fetchEbooks(id: String, params: Map[String, String]): Future[FetchResult] =
    ebookRegistry.ask(Fetch(id, params, _))

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
            respondWithHeaders(securityResponseHeaders) {
              onComplete(searchEbooks(params)) {
                case Success(result) =>
                  result.result match {
                    case Right(ebookList) =>
                      complete(ebookList)
                    case Left(failedRequest) =>
                      failedRequest match {
                        case failure: ValidationFailure =>
                          complete(HttpResponse(BadRequest, entity = failure.message))
                        case InternalFailure =>
                          complete(HttpResponse(ImATeapot, entity = teapotMessage))
                      }
                  }
                case Failure(e) =>
                  System.out.println(s"Error: $e")
                  complete(HttpResponse(ImATeapot, entity = teapotMessage))
              }
            }
          }
        }
      },
      path(Segment) { id =>
        get {
          parameterMap { params =>
            respondWithHeaders(securityResponseHeaders) {
              onComplete(fetchEbooks(id, params)) {
                case Success(result) =>
                  result.result match {
                    case Right(singleEbook) =>
                      complete(singleEbook)
                    case Left(failedRequest) =>
                      failedRequest match {
                        case failure: ValidationFailure =>
                          complete(HttpResponse(BadRequest, entity = failure.message))
                        case NotFoundFailure =>
                          complete(NotFound)
                        case InternalFailure =>
                          complete(HttpResponse(ImATeapot, entity = teapotMessage))
                      }
                  }
                case Failure(e) =>
                  System.out.println(s"Error: $e")
                  complete(HttpResponse(ImATeapot, entity = teapotMessage))
              }
            }
          }
        }
      }
    )

  lazy val healthCheckRoute: Route =
    get {
      complete(OK)
    }

  // @see https://cheatsheetseries.owasp.org/cheatsheets/REST_Security_Cheat_Sheet.html
  // @see https://cheatsheetseries.owasp.org/cheatsheets/Content_Security_Policy_Cheat_Sheet.html
  private val securityResponseHeaders = Seq(
    RawHeader("Content-Security-Policy",
      "default-src 'none'; script-src 'self'; frame-ancestors 'none'; form-action 'self'"),
    RawHeader("X-Content-Type-Options", "nosniff"),
    RawHeader("X-Frame-Options", "DENY")
  )

  private val teapotMessage: String = "There was an unexpected internal error. Please try again later."
}
