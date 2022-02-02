package dpla.ebookapi.v1.ebooks

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes.{BadRequest, ImATeapot}
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives.{complete, onComplete, respondWithHeaders}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import dpla.ebookapi.v1.ebooks.JsonFormats._
import dpla.ebookapi.v1.ebooks.ParamValidatorActor.ValidateSearchParams

import scala.collection.immutable
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

final case class SearchResult(result: Either[FailedRequest, EbookList])
final case class FetchResult(result: Either[FailedRequest, SingleEbook])

trait FailedRequest
case class ValidationFailure(message: String) extends FailedRequest
case object NotFoundFailure extends FailedRequest
case object InternalFailure extends FailedRequest

object EbookRegistry {

  sealed trait Command
  final case class Search(
                           params: Map[String, String],
                           replyTo: ActorRef[SearchResult]
                         ) extends Command
  final case class Fetch(
                          id: String,
                          params: Map[String, String],
                          replyTo: ActorRef[FetchResult]
                        ) extends Command

  def apply(): Behavior[Command] = {
    implicit val timeout: Timeout = 3.seconds


    Behaviors.setup { context =>
      val paramValidator = context.spawn(ParamValidatorActor(), "param validator")
      val elasticSearchClient = context.spawn(ElasticSearchClientActor(), "elastic search client")


      Behaviors.receiveMessage {
        case Search(params, replyTo) =>
          implicit val timeout: Timeout = 3.seconds
          paramValidator.ask(ValidateSearchParams(params, _))
          replyTo ! SearchResult(search(params))
          Behaviors.same
        case Fetch(id, params, replyTo) =>
          replyTo ! FetchResult(fetch(id, params))
          Behaviors.same
      }
    }
  }

  private def search(params: Map[String, String]): Either[FailedRequest, EbookList] = ???

  private def fetch(id: String, params: Map[String, String]): Either[FailedRequest, SingleEbook] = ???
}
