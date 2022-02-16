package dpla.ebookapi.v1.ebooks

import akka.NotUsed
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{Behaviors, LoggerOps}
import dpla.ebookapi.v1.PostgresClient.{FindApiKey, PostgresClientCommand}
import dpla.ebookapi.v1.ebooks.EbookMapper.{MapFetchResponse, MapSearchResponse, MapperCommand}
import dpla.ebookapi.v1.ebooks.ElasticSearchClient.{EsClientCommand, GetEsFetchResult, GetEsSearchResult}
import dpla.ebookapi.v1.ebooks.ParamValidator.{ValidateFetchParams, ValidateSearchParams, ValidationRequest}

/**
 * Handles the control flow for processing a request from Routes.
 * Its children include ParamValidator, EbookMapper, and session actors.
 * It also messages with ElasticSearchClient.
 */
sealed trait RegistryResponse
final case class SearchResult(result: EbookList) extends RegistryResponse
final case class FetchResult(result: SingleEbook) extends RegistryResponse
final case class ValidationFailure(message: String) extends RegistryResponse
case object ForbiddenFailure extends RegistryResponse
case object NotFoundFailure extends RegistryResponse
case object InternalFailure extends RegistryResponse

object EbookRegistry {

  sealed trait RegistryCommand

  final case class Search(
                           esClient: ActorRef[EsClientCommand],
                           rawParams: Map[String, String],
                           replyTo: ActorRef[RegistryResponse]
                         ) extends RegistryCommand

  final case class Fetch(
                          esClient: ActorRef[EsClientCommand],
                          id: String,
                          rawParams: Map[String, String],
                          replyTo: ActorRef[RegistryResponse]
                        ) extends RegistryCommand

  def apply(
             postgresClient: ActorRef[PostgresClientCommand]
           ): Behavior[RegistryCommand] = {

    Behaviors.setup[RegistryCommand] { context =>

      // Spawn children.
      val paramValidator: ActorRef[ValidationRequest] =
        context.spawn(
          ParamValidator(),
          "ParamValidator"
        )

      val mapper: ActorRef[MapperCommand] =
        context.spawn(
          EbookMapper(),
          "EbookMapper"
        )

      Behaviors.receiveMessage[RegistryCommand] {

        case Search(esClient, rawParams, replyTo) =>
          // Create a session child actor to process the request.
          val sessionChildActor =
            processSearch(rawParams, replyTo, paramValidator, esClient, postgresClient, mapper)
          context.spawnAnonymous(sessionChildActor)
          Behaviors.same

        case Fetch(esClient, id, rawParams, replyTo) =>
          // Create a session child actor to process the request.
          val sessionChildActor =
            processFetch(id, rawParams, replyTo, paramValidator, esClient, postgresClient, mapper)
          context.spawnAnonymous(sessionChildActor)
          Behaviors.same
      }
    }
  }

  /**
   * Per session actor behavior for handling a search request.
   * The session actor has its own internal state
   * and its own ActorRef for sending/receiving messages.
   */
  private def processSearch(
                             rawParams: Map[String, String],
                             replyTo: ActorRef[RegistryResponse],
                             paramValidator: ActorRef[ValidationRequest],
                             esClient: ActorRef[EsClientCommand],
                             postgresClient: ActorRef[PostgresClientCommand],
                             mapper: ActorRef[EbookMapper.MapperCommand],
                   ): Behavior[NotUsed] = {

    Behaviors.setup[AnyRef] { context =>

      var searchParams: Option[SearchParams] = None
      var searchResult: Option[EbookList] = None
      var authorized: Boolean = false

      // Send initial message to ParamValidator.
      paramValidator ! ValidateSearchParams(rawParams, context.self)
//      postgresClient ! FindApiKey("08e3918eeb8bf4469924f062072459a8", context.self)

      Behaviors.receiveMessage {

        /**
         * Possible responses from ParamValidator.
         * If params are valid, send a message to ElasticSearchClient to get a
         * search result.
         * If params are invalid, send an error message back to Routes.
         */
        case ValidSearchParams(apiKey: String, params: SearchParams) =>
          if (searchParams.isEmpty) {
            searchParams = Some(params)
            esClient ! GetEsSearchResult(params, context.self)
            postgresClient ! FindApiKey(apiKey, context.self)
            Behaviors.same
          }
          else {
            // Sanity check - search params should only be set once.
            context.log.error("Multiple attempts to set searchParams")
            replyTo ! InternalFailure
            Behaviors.stopped
          }

        case InvalidParams(message) =>
          replyTo ! ValidationFailure(message)
          Behaviors.stopped

        case InvalidApiKey =>
          replyTo ! ForbiddenFailure
          Behaviors.stopped

        /**
         * Possible responses from ElasticSearchClient.
         * If the search was successful, send a message to EbookMapper to get a
         * mapped EbookList.
         * If the search was unsuccessful, send an error message back to Routes.
         */
        case ElasticSearchSuccess(body) =>
          searchParams match {
            case Some(p) =>
              mapper ! MapSearchResponse(body, p.page, p.pageSize, context.self)
              Behaviors.same
            case None =>
              // This should not happen.
              context.log.error(
                "Cannot map ElasticSearch response b/c SearchParams are missing."
              )
              replyTo ! InternalFailure
              Behaviors.stopped
          }

        case ElasticSearchHttpFailure(statusCode) =>
          context.log.error2(
            "ElasticSearch search RESPONSE ERROR: {}: {}",
            statusCode.intValue,
            statusCode.reason
          )
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
         * If mapping was successful, send the mapped EbookList back to Routes.
         * If mapping was unsuccessful, send an error message back to Routes.
         */
        case MappedEbookList(ebookList) =>
          if (searchResult.isEmpty) {
            searchResult = Some(ebookList)
            replyTo ! SearchResult(ebookList)
            Behaviors.stopped
          }
          else {
            // Sanity check - searchResult should only be set once
            context.log.error("Multiple attempts to set searchResult")
            replyTo ! InternalFailure
            Behaviors.stopped
          }


        case MapFailure =>
          replyTo ! InternalFailure
          Behaviors.stopped

        case _ =>
          Behaviors.unhandled
      }
    }.narrow[NotUsed]
  }

  /**
   * Per session actor behavior for handling a fetch request.
   * The session actor has its own internal state
   * and its own ActorRef for sending/receiving messages.
   */
  private def processFetch(
                            id: String,
                            rawParams: Map[String, String],
                            replyTo: ActorRef[RegistryResponse],
                            paramValidator: ActorRef[ValidationRequest],
                            esClient: ActorRef[EsClientCommand],
                            postgresClient: ActorRef[PostgresClientCommand],
                            mapper: ActorRef[MapperCommand],
                   ): Behavior[NotUsed] = {

    Behaviors.setup[AnyRef] { context =>

      var fetchResult: Option[SingleEbook] = None
      var authorized: Boolean = false

      // Send initial message to ParamValidator.
      paramValidator ! ValidateFetchParams(id, rawParams, context.self)

      Behaviors.receiveMessage {
        /**
         * Possible responses from ParamValidator.
         * If params are valid, send a message to ElasticSearchClient to get a
         * fetch result.
         * If params are invalid, send an error message back to Routes.
         */
        case ValidFetchParams(apiKey: String, params: FetchParams) =>
          esClient ! GetEsFetchResult(params, context.self)
          postgresClient ! FindApiKey(apiKey, context.self)
          Behaviors.same

        case InvalidParams(message) =>
          replyTo ! ValidationFailure(message)
          Behaviors.stopped

        case InvalidApiKey =>
          replyTo ! ForbiddenFailure
          Behaviors.stopped

        /**
         * Possible responses from ElasticSearchClient.
         * If the fetch was successful, send a message to EbookMapper to get a
         * mapped Ebook.
         * If the fetch was unsuccessful, send an error message back to Routes.
         */
        case ElasticSearchSuccess(body) =>
          mapper ! MapFetchResponse(body, context.self)
          Behaviors.same

        case ElasticSearchHttpFailure(statusCode) =>
          if (statusCode.intValue == 404)
            replyTo ! NotFoundFailure
          else {
            context.log.error2(
              "ElasticSearch fetch RESPONSE ERROR: {}: {}",
              statusCode.intValue,
              statusCode.reason
            )
            replyTo ! InternalFailure
          }
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
          if (fetchResult.isEmpty) {
            replyTo ! FetchResult(singleEbook)
            Behaviors.stopped
          }
          else {
            // Sanity check - fetchResult should only be set once
            context.log.error("Multiple attempts to set fetchResult.")
            replyTo ! InternalFailure
            Behaviors.stopped
          }

        case MapFailure =>
          replyTo ! InternalFailure
          Behaviors.stopped

        case _ =>
          Behaviors.unhandled
      }
    }.narrow[NotUsed]
  }
}
