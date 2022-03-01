package dpla.ebookapi

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import dpla.ebookapi.v1.ebooks.EbookRegistry.{EbookRegistryCommand, Fetch, Search}
import dpla.ebookapi.v1.ebooks.{FetchResult, SearchResult}

import scala.concurrent.Future
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.headers.RawHeader

import scala.util.{Failure, Success}
import dpla.ebookapi.v1.ebooks.JsonFormats._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import dpla.ebookapi.v1.{ForbiddenFailure, InternalFailure, NotFoundFailure, RegistryResponse, ValidationFailure}
import dpla.ebookapi.v1.apiKey.{ApiKeyRegistryCommand, CreateApiKey, DisabledApiKey, ExistingApiKey, NewApiKey}
import org.slf4j.{Logger, LoggerFactory}


class Routes(
              ebookRegistry: ActorRef[EbookRegistryCommand],
              apiKeyRegistry: ActorRef[ApiKeyRegistryCommand]
            )(implicit val system: ActorSystem[_]) {

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout: Timeout = Timeout.create(
      system.settings.config.getDuration("application.routes.askTimeout")
  )

  val log: Logger = LoggerFactory.getLogger("dpla.ebookapi.Routes")

  // Search and fetch requests are send to EbookRegistry actor for processing.
  def searchEbooks(
                    params: Map[String, String],
                    host: String,
                    path: String
                  ): Future[RegistryResponse] =
    ebookRegistry.ask(Search(params, host, path, _))

  def fetchEbooks(
                   id: String,
                   params: Map[String, String],
                   host: String,
                   path: String
                 ): Future[RegistryResponse] =
    ebookRegistry.ask(Fetch(id, params, host, path, _))

  def createApiKey(email: String): Future[RegistryResponse] =
    apiKeyRegistry.ask(CreateApiKey(email, _))

  lazy val applicationRoutes: Route =
    concat (
      pathPrefix("ebooks")(ebooksRoutes),
      pathPrefix("v1") {
        concat(
          pathPrefix("ebooks")(ebooksRoutes),
          pathPrefix("api_key")(apiKeyRoute)
        )
      },
      path("health-check")(healthCheckRoute)
    )

  lazy val ebooksRoutes: Route =
    concat(
      pathEnd {
        get {
          extractHost { host =>
            extractMatchedPath { path =>
              parameterMap { params =>
                // Get the API key from Authorization header if it exists.
                optionalHeaderValueByName("Authorization") { auth =>
                  val updatedParams = auth match {
                    case Some(key) => params + ("api_key" -> key)
                    case None => params
                  }
                  respondWithHeaders(securityResponseHeaders) {
                    onComplete(searchEbooks(updatedParams, host, path.toString)) {
                      case Success(response) =>
                        response match {
                          case SearchResult(ebookList) =>
                            complete(ebookList)
                          case ForbiddenFailure =>
                            complete(HttpResponse(Forbidden, entity = forbiddenMessage))
                          case ValidationFailure(message) =>
                            complete(HttpResponse(BadRequest, entity = message))
                          case InternalFailure =>
                            complete(HttpResponse(ImATeapot, entity = teapotMessage))
                          case _ =>
                            log.error(
                              "Routes /ebooks received unexpected RegistryResponse {}",
                              response.getClass.getName
                            )
                            complete(HttpResponse(ImATeapot, entity = teapotMessage))
                        }
                      case Failure(e) =>
                        log.error(
                          "Routes /ebooks failed to get response from Registry:", e
                        )
                        complete(HttpResponse(ImATeapot, entity = teapotMessage))
                    }
                  }
                }
              }
            }
          }
        }
      },
      path(Segment) { id =>
        get {
          extractHost { host =>
            extractMatchedPath { path =>
              parameterMap { params =>
                // Get the API key from Authorization header if it exists.
                optionalHeaderValueByName("Authorization") { auth =>
                  val updatedParams = auth match {
                    case Some(key) => params + ("api_key" -> key)
                    case None => params
                  }
                  respondWithHeaders(securityResponseHeaders) {
                    onComplete(fetchEbooks(id, updatedParams, host, path.toString)) {
                      case Success(response) =>
                        response match {
                          case FetchResult(singleEbook) =>
                            complete(singleEbook)
                          case ForbiddenFailure =>
                            complete(HttpResponse(Forbidden, entity = forbiddenMessage))
                          case ValidationFailure(message) =>
                            complete(HttpResponse(BadRequest, entity = message))
                          case NotFoundFailure =>
                            complete(HttpResponse(NotFound, entity = notFoundMessage))
                          case InternalFailure =>
                            complete(HttpResponse(ImATeapot, entity = teapotMessage))
                          case _ =>
                            log.error(
                              "Routes /ebooks/[ID] received unexpected RegistryResponse {}",
                              response.getClass.getName
                            )
                            complete(HttpResponse(ImATeapot, entity = teapotMessage))
                        }
                      case Failure(e) =>
                        log.error(
                          "Routes /ebooks/[ID] failed to get response from Registry:",
                          e
                        )
                        complete(HttpResponse(ImATeapot, entity = teapotMessage))
                    }
                  }
                }
              }
            }
          }
        }
      }
    )

  lazy val apiKeyRoute: Route =
    path(Segment) { email =>
      post {
        respondWithHeaders(securityResponseHeaders) {
          onComplete(createApiKey(email)) {
            case Success(response) =>
              response match {
                case NewApiKey(email) =>
                  complete(newKeyMessage(email))
                case ExistingApiKey(email) =>
                  complete(
                    HttpResponse(Conflict, entity = existingKeyMessage(email))
                  )
                case DisabledApiKey(email) =>
                  complete(
                    HttpResponse(Conflict, entity = disabledKeyMessage(email))
                  )
                case ValidationFailure(message) =>
                  complete(HttpResponse(BadRequest, entity = message))
                case InternalFailure =>
                  complete(HttpResponse(ImATeapot, entity = teapotMessage))
              }
            case Failure(e) =>
              log.error(
                "Routes /api_key failed to get response from Registry:", e
              )
              complete(HttpResponse(ImATeapot, entity = teapotMessage))
          }
        }
      }
    }

  lazy val healthCheckRoute: Route =
    get {
      complete(OK)
    }

  // @see https://cheatsheetseries.owasp.org/cheatsheets/REST_Security_Cheat_Sheet.html
  // @see https://cheatsheetseries.owasp.org/cheatsheets/Content_Security_Policy_Cheat_Sheet.html
  private val securityResponseHeaders = Seq(
    RawHeader(
      "Content-Security-Policy",
      "default-src 'none'; script-src 'self'; frame-ancestors 'none'; form-action 'self'"
    ),
    RawHeader("X-Content-Type-Options", "nosniff"),
    RawHeader("X-Frame-Options", "DENY")
  )

  private val teapotMessage: String =
    "There was an unexpected internal error. Please try again later."

  private val notFoundMessage: String =
    "The ebook you are searching for could not be found."

  private val forbiddenMessage: String =
    "Invalid or inactive API key"

  private def existingKeyMessage(email: String): String =
    s"There is already an API key for $email. We have sent a reminder " +
      "message to that address."

  private def newKeyMessage(email: String): String =
    s"API key created and sent to $email"

  private def disabledKeyMessage(email: String): String =
    s"The API key associated with email address $email has been disabled. " +
      "If you would like to reactivate it, please contact DPLA."
}
