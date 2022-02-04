package dpla.ebookapi.v1.ebooks

import akka.NotUsed
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.ebookapi.v1.ebooks.EbookMapper.{MapFetchResponse, MapSearchResponse}
import dpla.ebookapi.v1.ebooks.ElasticSearchClient.{GetEsFetchResult, GetEsSearchResult}
import dpla.ebookapi.v1.ebooks.ParamValidator.{ValidateFetchParams, ValidateSearchParams}


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
        context.spawn(ParamValidator(), "ParamValidator")
      val elasticSearchClient: ActorRef[ElasticSearchClient.EsClientCommand] =
        context.spawn(ElasticSearchClient(), "ElasticSearchClient")
      val ebookMapper: ActorRef[EbookMapper.MapperCommand] =
        context.spawn(EbookMapper(), "EbookMapper")

      Behaviors.receiveMessage[RegistryCommand] {

        case Search(rawParams, replyTo) =>
          // Create a session child actor to process the request
          val sessionChildActor = processSearch(rawParams, replyTo, paramValidator, elasticSearchClient, ebookMapper)
          val uniqueId = java.util.UUID.randomUUID.toString
          context.spawn(sessionChildActor, s"ProcessSearchRequest-$uniqueId")
          Behaviors.same

        case Fetch(id, rawParams, replyTo) =>
          // Create a session child actor to process the request
          val sessionChildActor = processFetch(id, rawParams, replyTo, paramValidator, elasticSearchClient, ebookMapper)
          val uniqueId = java.util.UUID.randomUUID.toString
          context.spawn(sessionChildActor, s"ProcessFetchRequest-$uniqueId")
          Behaviors.same
      }
    }
  }

  /**
   * Per session actor behavior for handling a search request.
   * The session actor has its own internal state and its own ActorRef for sending/receiving messages.
   */
  def processSearch(
                     rawParams: Map[String, String],
                     replyTo: ActorRef[RegistryResponse],
                     paramValidator: ActorRef[ParamValidator.ValidationRequest],
                     elasticSearchClient: ActorRef[ElasticSearchClient.EsClientCommand],
                     ebookMapper: ActorRef[EbookMapper.MapperCommand],
                   ): Behavior[NotUsed] = {

    Behaviors.setup[AnyRef] { context =>

      var searchParams: Option[SearchParams] = None

      paramValidator ! ValidateSearchParams(rawParams, context.self)

      Behaviors.receiveMessage {
        case ValidSearchParams(params: SearchParams) =>
          searchParams = Some(params)
          elasticSearchClient ! GetEsSearchResult(params, context.self)
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

        case MappedEbookList(ebookList) =>
          replyTo ! SearchResult(ebookList)
          Behaviors.stopped

        case MapFailure =>
          replyTo ! InternalFailure
          Behaviors.stopped

        case _ =>
          // TODO log?
          Behaviors.unhandled
      }
    }.narrow[NotUsed]
  }

  /**
   * Per session actor behavior for handling a fetch request.
   * The session actor has its own internal state and its own ActorRef for sending/receiving messages.
   */
  def processFetch(
                     id: String,
                     rawParams: Map[String, String],
                     replyTo: ActorRef[RegistryResponse],
                     paramValidator: ActorRef[ParamValidator.ValidationRequest],
                     elasticSearchClient: ActorRef[ElasticSearchClient.EsClientCommand],
                     ebookMapper: ActorRef[EbookMapper.MapperCommand],
                   ): Behavior[NotUsed] = {

    Behaviors.setup[AnyRef] { context =>

      paramValidator ! ValidateFetchParams(id, rawParams, context.self)

      Behaviors.receiveMessage {
        case ValidFetchParams(params: FetchParams) =>
          elasticSearchClient ! GetEsFetchResult(params, context.self)
          Behaviors.same

        case ValidationError(message) =>
          replyTo ! ValidationFailure(message)
          Behaviors.stopped

        case EsSuccess(body) =>
          ebookMapper ! MapFetchResponse(body, context.self)
          Behaviors.same

        case EsHttpFailure(statusCode) =>
          if (statusCode.intValue == 404)
            replyTo ! NotFoundFailure
          else
            replyTo ! InternalFailure
          Behaviors.stopped

        case EsUnreachable =>
          replyTo ! InternalFailure
          Behaviors.stopped

        case MappedSingleEbook(singleEbook) =>
          replyTo ! FetchResult(singleEbook)
          Behaviors.stopped

        case MapFailure =>
          replyTo ! InternalFailure
          Behaviors.stopped

        case _ =>
          // TODO log?
          Behaviors.unhandled
      }
    }.narrow[NotUsed]
  }
}
