package dpla.ebookapi.v1.ebooks

import akka.NotUsed
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes.{BadRequest, ImATeapot}
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives.{complete, onComplete, respondWithHeaders}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import dpla.ebookapi.v1.ebooks.EbookMapper.MapSearchResponse
import dpla.ebookapi.v1.ebooks.ElasticSearchClient.GetSearchResult
import dpla.ebookapi.v1.ebooks.JsonFormats._
import dpla.ebookapi.v1.ebooks.ParamValidator.ValidateSearchParams

import scala.collection.immutable
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

sealed trait RegistryResponse
final case class SearchResult(result: EbookList) extends RegistryResponse
final case class FetchResult(result: SingleEbook) extends RegistryResponse
case class ValidationFailure(message: String) extends RegistryResponse
case object NotFoundFailure extends RegistryResponse
case object InternalFailure extends RegistryResponse

object EbookRegistry {

  sealed trait RegistryCommand

  final case class Search(
                           rawParams: Map[String, String],
                           replyTo: ActorRef[RegistryResponse]
                         ) extends RegistryCommand

  final case class Fetch(
                          id: String,
                          rawParams: Map[String, String],
                          replyTo: ActorRef[RegistryResponse]
                        ) extends RegistryCommand

  def apply(): Behavior[RegistryCommand] = {
    Behaviors.setup[RegistryCommand] { context =>
      val paramValidator: ActorRef[ParamValidator.ValidationRequest] =
        context.spawn(ParamValidator(), "param validator")
      val elasticSearchClient: ActorRef[ElasticSearchClient.EsClientCommand] =
        context.spawn(ElasticSearchClient(), "elastic search client")
      val ebookMapper: ActorRef[EbookMapper.MapperCommand] =
        context.spawn(EbookMapper(), "ebook mapper")

      Behaviors.receiveMessage[RegistryCommand] {

        case Search(rawParams, replyTo) =>

          // Create a session child actor to process the request
          val sessionChildActor = processSearch(rawParams, replyTo, paramValidator, elasticSearchClient, ebookMapper)
          context.spawn(sessionChildActor, "process search")
          Behaviors.same

        case Fetch(id, rawParams, replyTo) =>
          // TODO
          Behaviors.same
      }
    }
  }

  // Per session actor behavior
  // TODO is there a way to test this to make sure the child actor has its own ActorRef, different from parent?
  def processSearch(
                     rawParams: Map[String, String],
                     replyTo: ActorRef[RegistryResponse],
                     paramValidator: ActorRef[ParamValidator.ValidationRequest],
                     elasticSearchClient: ActorRef[ElasticSearchClient.EsClientCommand],
                     ebookMapper: ActorRef[EbookMapper.MapperCommand],
                   ): Behavior[NotUsed] = {

    Behaviors.setup[AnyRef] { context =>

      var searchParams: Option[SearchParams] = None

      // TODO add narrow to context.self?  Yes if separate processes for search and fetch.
      paramValidator ! ValidateSearchParams(rawParams, context.self)

      Behaviors.receiveMessage {
        case ValidSearchParams(params: SearchParams) =>
          searchParams = Some(params)
          elasticSearchClient ! GetSearchResult(params, context.self)
          Behaviors.same

        case ValidationError(message) =>
          replyTo ! ValidationFailure(message)
          Behaviors.stopped

        case EsSuccess(body) =>
          searchParams match {
            case Some(params) =>
              ebookMapper ! MapSearchResponse(body, params.page, params.pageSize, context.self)
              Behaviors.same
            case None =>
              // TODO log error
              replyTo ! InternalFailure
              Behaviors.stopped
          }

        case EsHttpFailure =>
          replyTo ! InternalFailure
          Behaviors.stopped

        case EsUnreachable =>
          replyTo ! InternalFailure
          Behaviors.stopped

        case _ =>
          Behaviors.unhandled
      }
    }.narrow[NotUsed]
  }
}
