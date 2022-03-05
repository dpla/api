package dpla.ebookapi.v1.ebooks

import akka.NotUsed
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, LoggerOps}
import dpla.ebookapi.v1.AnalyticsClient.{AnalyticsClientCommand, TrackFetch, TrackSearch}
import dpla.ebookapi.v1.{ForbiddenFailure, InternalFailure, InvalidParams, NotFoundFailure, RegistryResponse, ValidationFailure}
import dpla.ebookapi.v1.authentication.PostgresClient.{FindUserByKey, PostgresClientCommand}
import dpla.ebookapi.v1.ebooks.EbookMapper.{MapFetchResponse, MapSearchResponse, MapperCommand}
import dpla.ebookapi.v1.ebooks.ElasticSearchClient.{EsClientCommand, GetEsFetchResult, GetEsSearchResult}
import EbookParamValidator.{EbookValidationCommand, ValidateFetchParams, ValidateSearchParams}
import dpla.ebookapi.v1.authentication.{Account, UserFound, UserNotFound, PostgresError}


final case class SearchResult(result: EbookList) extends RegistryResponse
final case class FetchResult(result: SingleEbook) extends RegistryResponse

sealed trait EbookRegistryCommand

final case class SearchEbooks(
                               rawParams: Map[String, String],
                               host: String,
                               path: String,
                               replyTo: ActorRef[RegistryResponse]
                             ) extends EbookRegistryCommand

final case class FetchEbook(
                             id: String,
                             rawParams: Map[String, String],
                             host: String,
                             path: String,
                             replyTo: ActorRef[RegistryResponse]
                           ) extends EbookRegistryCommand

trait EbookRegistryBehavior {

  def spawnParamValidator(context: ActorContext[EbookRegistryCommand]):
    ActorRef[EbookValidationCommand]

  def spawnAuthenticationClient(context: ActorContext[EbookRegistryCommand]):
    ActorRef[PostgresClientCommand]

  def spawnSearchIndexClient(context: ActorContext[EbookRegistryCommand]):
    ActorRef[EsClientCommand]

  def spawnEbookMapper(context: ActorContext[EbookRegistryCommand]):
    ActorRef[MapperCommand]

  def spawnAnalyticsClient(context: ActorContext[EbookRegistryCommand]):
    ActorRef[AnalyticsClientCommand]

  def apply(): Behavior[EbookRegistryCommand] = {

    Behaviors.setup[EbookRegistryCommand] { context =>

      // Spawn children.
      val paramValidator: ActorRef[EbookValidationCommand] =
        spawnParamValidator(context)

      val authenticationClient: ActorRef[PostgresClientCommand] =
        spawnAuthenticationClient(context)

      val searchIndexClient: ActorRef[EsClientCommand] =
        spawnSearchIndexClient(context)

      val mapper: ActorRef[MapperCommand] =
        spawnEbookMapper(context)

      val analyticsClient: ActorRef[AnalyticsClientCommand] =
        spawnAnalyticsClient(context)

      Behaviors.receiveMessage[EbookRegistryCommand] {

        case SearchEbooks(rawParams, host, path, replyTo) =>
          // Create a session child actor to process the request.
          val sessionChildActor =
            processSearch(rawParams, host, path, replyTo, paramValidator,
              searchIndexClient, authenticationClient, mapper, analyticsClient)
          context.spawnAnonymous(sessionChildActor)
          Behaviors.same

        case FetchEbook(id, rawParams, host, path, replyTo) =>
          // Create a session child actor to process the request.
          val sessionChildActor =
            processFetch(id, rawParams, host, path, replyTo, paramValidator,
              searchIndexClient, authenticationClient, mapper, analyticsClient)
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
                             host: String,
                             path: String,
                             replyTo: ActorRef[RegistryResponse],
                             paramValidator: ActorRef[EbookValidationCommand],
                             searchIndexClient: ActorRef[EsClientCommand],
                             authenticationClient: ActorRef[PostgresClientCommand],
                             mapper: ActorRef[MapperCommand],
                             analyticsClient: ActorRef[AnalyticsClientCommand]
                           ): Behavior[NotUsed] = {

    Behaviors.setup[AnyRef] { context =>

      // store variables that will be needed for multiple tasks
      var searchParams: Option[SearchParams] = None
      var apiKey: Option[String] = None

      // Both searchResult and authorizedAccount must be present before sending
      // a response back to Routes.
      var searchResult: Option[EbookList] = None
      var authorizedAccount: Option[Account] = None

      var searchResultMessage: Option[RegistryResponse] = None

      // This behavior is invoked if either the API key has been authorized
      // or the mapping has been complete.
      def possibleSessionResolution(): Behavior[AnyRef] =
        (searchResult, authorizedAccount) match {
          case (Some(ebookList), Some(account)) =>
            // We've received both message replies, time to complete session
            // Send final result back to Routes
            replyTo ! SearchResult(ebookList)

            // If not a staff/internal account, send to analytics tracker
            if (!account.staff.getOrElse(false) && !account.email.endsWith("@dp.la")) {
              apiKey match {
                case Some(key) =>
                  analyticsClient ! TrackSearch(key, rawParams, host, path,
                    ebookList.docs)
                case None =>
                // no-op (this should not happen)
              }
            }
            Behaviors.stopped
          case _ =>
            // Still waiting for one of the message replies
            Behaviors.same
        }

      // Send initial message to ParamValidator.
      paramValidator ! ValidateSearchParams(rawParams, context.self)

      Behaviors.receiveMessage {

        /**
         * Possible responses from ParamValidator.
         * If params are valid, send a message to ElasticSearchClient to get a
         * search result.
         * If params are invalid, send an error message back to Routes.
         */
        case ValidSearchParams(key: String, params: SearchParams) =>
          if (searchParams.isEmpty) {
            searchParams = Some(params)
            apiKey = Some(key)
            // Calls to Postgres and ElasticSearch can happen concurrently.
            authenticationClient ! FindUserByKey(key, context.self)
            searchIndexClient ! GetEsSearchResult(params, context.self)
          }
          Behaviors.same

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
         * If mapping was successful (and API key has been authorized),
         * send the mapped EbookList back to Routes.
         * If mapping was unsuccessful, send an error message back to Routes.
         */
        case MappedEbookList(ebookList) =>
          if (searchResult.isEmpty) {
            searchResult = Some(ebookList)
            possibleSessionResolution()
          }
          else
            Behaviors.same

        case MapFailure =>
          replyTo ! InternalFailure
          Behaviors.stopped

        /**
         * Possible responses from PostgresClient.
         * If API key was found (and a successful mapping has been received),
         * send mapped ebookList to Routes.
         * Otherwise, send an error to Routes.
         */

        case UserFound(account) =>
          if(account.enabled.getOrElse(true))
            if (authorizedAccount.isEmpty) {
              authorizedAccount = Some(account)
              possibleSessionResolution()
            }
            else
              Behaviors.same
          else {
            replyTo ! ForbiddenFailure
            Behaviors.stopped
          }

        case UserNotFound =>
          replyTo ! ForbiddenFailure
          Behaviors.stopped

        case PostgresError =>
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
                            host: String,
                            path: String,
                            replyTo: ActorRef[RegistryResponse],
                            paramValidator: ActorRef[EbookValidationCommand],
                            esClient: ActorRef[EsClientCommand],
                            postgresClient: ActorRef[PostgresClientCommand],
                            mapper: ActorRef[MapperCommand],
                            analyticsClient: ActorRef[AnalyticsClientCommand]
                          ): Behavior[NotUsed] = {

    Behaviors.setup[AnyRef] { context =>

      // store variables that will be needed for multiple tasks
      var apiKey: Option[String] = None

      // Both fetchResult and authorizedAccount must be present before sending
      // a response back to Routes.
      var fetchResult: Option[SingleEbook] = None
      var authorizedAccount: Option[Account] = None

      // This behavior is invoked if either the API key has been authorized
      // or the mapping has been complete.
      def possibleSessionResolution(): Behavior[AnyRef] =
        (fetchResult, authorizedAccount) match {
          case (Some(singleEbook), Some(account)) =>
            // We've received both message replies, time to complete session
            replyTo ! FetchResult(singleEbook)

            // If not a staff/internal account, send to analytics tracker
            if (!account.staff.getOrElse(false) && !account.email.endsWith("@dp.la")) {
              apiKey match {
                case Some(key) =>
                  analyticsClient ! TrackFetch(key, host, path,
                    singleEbook.docs.headOption)
                case None =>
                // no-op (this should not happen)
              }
            }

            Behaviors.stopped
          case _ =>
            // Still waiting for one of the message replies
            Behaviors.same
        }

      // Send initial message to ParamValidator.
      paramValidator ! ValidateFetchParams(id, rawParams, context.self)

      Behaviors.receiveMessage {
        /**
         * Possible responses from ParamValidator.
         * If params are valid, send a message to ElasticSearchClient to get a
         * fetch result.
         * If params are invalid, send an error message back to Routes.
         */
        case ValidFetchParams(key: String, params: FetchParams) =>
          apiKey = Some(key)
          // Calls to Postgres and ElasticSearch can happen concurrently.
          postgresClient ! FindUserByKey(key, context.self)
          esClient ! GetEsFetchResult(params, context.self)
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
         * If the mapping was successful (and the API key has been authorized),
         * send the mapped Ebook back to Routes.
         * If the mapping was unsuccessful, send an error message back to Routes.
         */
        case MappedSingleEbook(singleEbook) =>
          if (fetchResult.isEmpty) {
            fetchResult = Some(singleEbook)
            possibleSessionResolution()
          }
          else
            Behaviors.same

        case MapFailure =>
          replyTo ! InternalFailure
          Behaviors.stopped

        /**
         * Possible responses from PostgresClient.
         * If API key was found (and a successful mapping has been received),
         * send mapped ebook to Routes.
         * Otherwise, send an error to Routes.
         */

        case UserFound(account) =>
          if (account.enabled.getOrElse(true))
            if (authorizedAccount.isEmpty) {
              authorizedAccount = Some(account)
              possibleSessionResolution()
            }
            else
              Behaviors.same
          else {
            replyTo ! ForbiddenFailure
            Behaviors.stopped
          }

        case UserNotFound =>
          replyTo ! ForbiddenFailure
          Behaviors.stopped

        case PostgresError =>
          replyTo ! InternalFailure
          Behaviors.stopped

        case _ =>
          Behaviors.unhandled
      }
    }.narrow[NotUsed]
  }
}
