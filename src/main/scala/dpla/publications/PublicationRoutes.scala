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
            // TODO THIS IS WHERE I LEFT OFF
            parameters("page".optional, "page_size".optional) { (page, pageSize) =>
              val params = validSearchParams(page, pageSize)
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

  private def validSearchParams(page: Option[String], pageSize: Option[String]): SearchParams = {
    SearchParams(
      page = validPage(page),
      pageSize = validPageSize(pageSize)
    )
  }

  private def toIntOpt(str: String): Option[Int] =
    try {
      Some(str.toInt)
    } catch {
      case _: Exception => None
    }

  // Must be an integer greater than 0, defaults to 1
  private def validPage(page: Option[String]): Int =
    page.flatMap(toIntOpt) match {
      case Some(int) =>
        if (int == 0) 1 else int
      case None => 1
    }

  // Must be an integer between 0 and 1000, defaults to 10
  private def validPageSize(pageSize: Option[String]): Int =
    pageSize.flatMap(toIntOpt) match {
      case Some(int) =>
        if (int > 1000) 1000 else int
      case None => 10
    }
}
