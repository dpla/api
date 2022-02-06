package dpla.ebookapi.v1.ebooks

import akka.NotUsed
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.ebookapi.v1.ebooks.EbookMapper.{MapFetchResponse, MapSearchResponse}
import dpla.ebookapi.v1.ebooks.ElasticSearchClient.{EsClientCommand, GetEsFetchResult, GetEsSearchResult}
import dpla.ebookapi.v1.ebooks.ParamValidator.{ValidateFetchParams, ValidateSearchParams}

/**
 * Handles the control flow for processing a request from Routes.
 * It's children include ParamValidator, EbookMapper, and session actors.
 * It also messages with ElasticSearchClient.
 */
sealed trait RegistryResponse
final case class SearchResult(result: EbookList) extends RegistryResponse
final case class FetchResult(result: SingleEbook) extends RegistryResponse
case class ValidationFailure(message: String) extends RegistryResponse
case object NotFoundFailure extends RegistryResponse
case object InternalFailure extends RegistryResponse

object EbookRegistry {

  sealed trait RegistryCommand

  final case class Search(
                           client: ActorRef[EsClientCommand],
                           rawParams: Map[String, String],
                           replyTo: ActorRef[RegistryResponse]
                         ) extends RegistryCommand

  final case class Fetch(
                          client: ActorRef[EsClientCommand],
                          id: String,
                          rawParams: Map[String, String],
                          replyTo: ActorRef[RegistryResponse]
                        ) extends RegistryCommand

  def apply(): Behavior[RegistryCommand] = {
    Behaviors.setup[RegistryCommand] { context =>

      // Spawn children.
      val paramValidator: ActorRef[ParamValidator.ValidationRequest] =
        context.spawn(ParamValidator(), "ParamValidator")
      val mapper: ActorRef[EbookMapper.MapperCommand] =
        context.spawn(EbookMapper(), "EbookMapper")

      Behaviors.receiveMessage[RegistryCommand] {

        case Search(client, rawParams, replyTo) =>
          // Create a session child actor to process the request.
          val sessionChildActor =
            processSearch(rawParams, replyTo, paramValidator, client, mapper)
          context.spawnAnonymous(sessionChildActor)
          Behaviors.same

        case Fetch(client, id, rawParams, replyTo) =>
          // Create a session child actor to process the request.
          val sessionChildActor =
            processFetch(id, rawParams, replyTo, paramValidator, client, mapper)
          context.spawnAnonymous(sessionChildActor)
          Behaviors.same
      }
    }
  }

  /**
   * Per session actor behavior for handling a search request.
   * The session actor has its own internal state and its own ActorRef for sending/receiving messages.
   */
  private def processSearch(
                     rawParams: Map[String, String],
                     replyTo: ActorRef[RegistryResponse],
                     paramValidator: ActorRef[ParamValidator.ValidationRequest],
                     client: ActorRef[ElasticSearchClient.EsClientCommand],
                     mapper: ActorRef[EbookMapper.MapperCommand],
                   ): Behavior[NotUsed] = {

    Behaviors.setup[AnyRef] { context =>

      var searchParams: Option[SearchParams] = None

      // Send initial message to ParamValidator.
      paramValidator ! ValidateSearchParams(rawParams, context.self)

      Behaviors.receiveMessage {

        /**
         * Possible responses from ParamValidator.
         * If params are valid, send a message to ElasticSearchClient to get a search result.
         * If params are invalid, send an error message back to Routes.
         */
        case ValidSearchParams(params: SearchParams) =>
          searchParams = Some(params)
          client ! GetEsSearchResult(params, context.self)
          Behaviors.same

        case InvalidParams(message) =>
          replyTo ! ValidationFailure(message)
          Behaviors.stopped

        /**
         * Possible responses from ElasticSearchClient.
         * If the search was successful, send a message to EbookMapper to get a mapped EbookList.
         * If the search was unsuccessful, send an error message back to Routes.
         */
        case ElasticSearchSuccess(body) =>
          searchParams match {
            case Some(params) =>
              mapper ! MapSearchResponse(body, params.page, params.pageSize, context.self)
              Behaviors.same
            case None =>
              // TODO log error
              replyTo ! InternalFailure
              Behaviors.stopped
          }

        case ElasticSearchHttpFailure(_) =>
          replyTo ! InternalFailure
          Behaviors.stopped

        case ElasticSearchParseFailure =>
          replyTo ! InternalFailure
          Behaviors.stopped

        case ElasticSearchUnreachable =>
          replyTo ! InternalFailure
          Behaviors.stopped

        /**
         * Possible responses from EbookMapper.
         * If the mapping was successful, send the mapped EbookList back to Routes.
         * If the mapping was unsuccessful, send an error message back to Routes.
         */
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
  private def processFetch(
                     id: String,
                     rawParams: Map[String, String],
                     replyTo: ActorRef[RegistryResponse],
                     paramValidator: ActorRef[ParamValidator.ValidationRequest],
                     client: ActorRef[ElasticSearchClient.EsClientCommand],
                     mapper: ActorRef[EbookMapper.MapperCommand],
                   ): Behavior[NotUsed] = {

    Behaviors.setup[AnyRef] { context =>

      // Send initial message to ParamValidator.
      paramValidator ! ValidateFetchParams(id, rawParams, context.self)

      Behaviors.receiveMessage {
        /**
         * Possible responses from ParamValidator.
         * If params are valid, send a message to ElasticSearchClient to get a fetch result.
         * If params are invalid, send an error message back to Routes.
         */
        case ValidFetchParams(params: FetchParams) =>
          client ! GetEsFetchResult(params, context.self)
          Behaviors.same

        case InvalidParams(message) =>
          replyTo ! ValidationFailure(message)
          Behaviors.stopped

        /**
         * Possible responses from ElasticSearchClient.
         * If the fetch was successful, send a message to EbookMapper to get a mapped Ebook.
         * If the fetch was unsuccessful, send an error message back to Routes.
         */
        case ElasticSearchSuccess(body) =>
          mapper ! MapFetchResponse(body, context.self)
          Behaviors.same

        case ElasticSearchHttpFailure(status) =>
          if (status == 404)
            replyTo ! NotFoundFailure
          else
            replyTo ! InternalFailure
          Behaviors.stopped

        case ElasticSearchParseFailure =>
          replyTo ! InternalFailure
          Behaviors.stopped

        case ElasticSearchUnreachable =>
          replyTo ! InternalFailure
          Behaviors.stopped

        /**
         * Possible responses from EbookMapper.
         * If the mapping was successful, send the mapped Ebook back to Routes.
         * If the mapping was unsuccessful, send an error message back to Routes.
         */
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
