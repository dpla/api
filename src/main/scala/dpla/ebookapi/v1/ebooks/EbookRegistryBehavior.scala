package dpla.ebookapi.v1.ebooks

import akka.NotUsed
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, LoggerOps}
import dpla.ebookapi.v1.AnalyticsClient.{AnalyticsClientCommand, TrackFetch, TrackSearch}
import dpla.ebookapi.v1.{ForbiddenFailure, InternalFailure, NotFoundFailure, RegistryResponse, ValidationFailure}
import dpla.ebookapi.v1.ebooks.EbookMapper.{MapFetchResponse, MapSearchResponse, MapperCommand}
import dpla.ebookapi.v1.ebooks.ElasticSearchClient.{EsClientCommand, GetEsFetchResult, GetEsSearchResult}
import EbookParamValidator.{EbookParamValidatorCommand, ValidateFetchParams, ValidateSearchParams}
import dpla.ebookapi.v1.authentication.{Account, AuthenticatorCommand, AuthenticatorFailure, Authorize, Authorized, NotAuthorized}


final case class SearchResult(result: EbookList) extends RegistryResponse
final case class FetchResult(result: SingleEbook) extends RegistryResponse

sealed trait EbookRegistryCommand

final case class SearchEbooks(
                               apiKey: Option[String],
                               rawParams: Map[String, String],
                               host: String,
                               path: String,
                               replyTo: ActorRef[RegistryResponse]
                             ) extends EbookRegistryCommand

final case class FetchEbook(
                             apiKey: Option[String],
                             id: String,
                             rawParams: Map[String, String],
                             host: String,
                             path: String,
                             replyTo: ActorRef[RegistryResponse]
                           ) extends EbookRegistryCommand

trait EbookRegistryBehavior {

  def spawnAuthenticator(context: ActorContext[EbookRegistryCommand]):
    ActorRef[AuthenticatorCommand]

  def spawnParamValidator(context: ActorContext[EbookRegistryCommand]):
    ActorRef[EbookParamValidatorCommand]

  def spawnSearchIndexClient(context: ActorContext[EbookRegistryCommand]):
    ActorRef[EsClientCommand]

  def spawnEbookMapper(context: ActorContext[EbookRegistryCommand]):
    ActorRef[MapperCommand]

  def spawnAnalyticsClient(context: ActorContext[EbookRegistryCommand]):
    ActorRef[AnalyticsClientCommand]

  def apply(): Behavior[EbookRegistryCommand] = {

    Behaviors.setup[EbookRegistryCommand] { context =>

      // Spawn children.

      val authenticator: ActorRef[AuthenticatorCommand] =
        spawnAuthenticator(context)

      val paramValidator: ActorRef[EbookParamValidatorCommand] =
        spawnParamValidator(context)

      val searchIndexClient: ActorRef[EsClientCommand] =
        spawnSearchIndexClient(context)

      val mapper: ActorRef[MapperCommand] =
        spawnEbookMapper(context)

      val analyticsClient: ActorRef[AnalyticsClientCommand] =
        spawnAnalyticsClient(context)

      Behaviors.receiveMessage[EbookRegistryCommand] {

        case SearchEbooks(apiKey, rawParams, host, path, replyTo) =>
          // Create a session child actor to process the request.
          val sessionChildActor =
            processSearch(apiKey, rawParams, host, path, replyTo, authenticator,
              paramValidator, searchIndexClient, mapper, analyticsClient)
          context.spawnAnonymous(sessionChildActor)
          Behaviors.same

        case FetchEbook(apiKey, id, rawParams, host, path, replyTo) =>
          // Create a session child actor to process the request.
          val sessionChildActor =
            processFetch(apiKey, id, rawParams, host, path, replyTo,
              authenticator, paramValidator, searchIndexClient, mapper, analyticsClient)
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
                             apiKey: Option[String],
                             rawParams: Map[String, String],
                             host: String,
                             path: String,
                             replyTo: ActorRef[RegistryResponse],
                             authenticator: ActorRef[AuthenticatorCommand],
                             paramValidator: ActorRef[EbookParamValidatorCommand],
                             searchIndexClient: ActorRef[EsClientCommand],
                             mapper: ActorRef[MapperCommand],
                             analyticsClient: ActorRef[AnalyticsClientCommand]
                           ): Behavior[NotUsed] = {

    Behaviors.setup[AnyRef] { context =>

      // store variables that will be needed for multiple tasks
      var searchParams: Option[SearchParams] = None

      // Both searchResult and authorizedAccount must be present before sending
      // a response back to Routes.
      var searchResult: Option[EbookList] = None
      var authorizedAccount: Option[Account] = None

      // This behavior is invoked if either the API key has been authorized
      // or the mapping has been complete.
      def possibleSessionResolution: Behavior[AnyRef] =
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

      // TODO change authenticator so it accepts Option for apiKey
      // Send initial messages
      authenticator ! Authorize(apiKey.getOrElse(""), context.self)
      paramValidator ! ValidateSearchParams(rawParams, context.self)

      Behaviors.receiveMessage {

        /**
         * Possible responses from Authenticator.
         */

        case Authorized(account) =>
          authorizedAccount = Some(account)
          possibleSessionResolution

        case NotAuthorized =>
          replyTo ! ForbiddenFailure
          Behaviors.stopped

        case AuthenticatorFailure =>
          replyTo ! InternalFailure
          Behaviors.stopped

        /**
         * Possible responses from ParamValidator.
         * If params are valid, send a message to ElasticSearchClient to get a
         * search result.
         * If params are invalid, send an error message back to Routes.
         */
        case ValidSearchParams(params: SearchParams) =>
          if (searchParams.isEmpty) {
            searchParams = Some(params)
            searchIndexClient ! GetEsSearchResult(params, context.self)
          }
          Behaviors.same

        case InvalidEbookParams(message) =>
          replyTo ! ValidationFailure(message)
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
            possibleSessionResolution
          }
          else
            Behaviors.same

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
                            apiKey: Option[String],
                            id: String,
                            rawParams: Map[String, String],
                            host: String,
                            path: String,
                            replyTo: ActorRef[RegistryResponse],
                            authenticator: ActorRef[AuthenticatorCommand],
                            paramValidator: ActorRef[EbookParamValidatorCommand],
                            esClient: ActorRef[EsClientCommand],
                            mapper: ActorRef[MapperCommand],
                            analyticsClient: ActorRef[AnalyticsClientCommand]
                          ): Behavior[NotUsed] = {

    Behaviors.setup[AnyRef] { context =>

      // Both fetchResult and authorizedAccount must be present before sending
      // a response back to Routes.
      var fetchResult: Option[SingleEbook] = None
      var authorizedAccount: Option[Account] = None

      // This behavior is invoked if either the API key has been authorized
      // or the mapping has been complete.
      def possibleSessionResolution: Behavior[AnyRef] =
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

      // TODO change authenticator so it accepts Option for apiKey
      // Send initial messages.
      authenticator ! Authorize(apiKey.getOrElse(""), context.self)
      paramValidator ! ValidateFetchParams(id, rawParams, context.self)

      Behaviors.receiveMessage {

        /**
         * Possible responses from Authenticator.
         */

        case Authorized(account) =>
          authorizedAccount = Some(account)
          possibleSessionResolution

        case NotAuthorized =>
          replyTo ! ForbiddenFailure
          Behaviors.stopped

        case AuthenticatorFailure =>
          replyTo ! InternalFailure
          Behaviors.stopped

        /**
         * Possible responses from ParamValidator.
         * If params are valid, send a message to ElasticSearchClient to get a
         * fetch result.
         * If params are invalid, send an error message back to Routes.
         */
        case ValidFetchParams(params) =>
          // Calls to Postgres and ElasticSearch can happen concurrently.
          esClient ! GetEsFetchResult(params, context.self)
          Behaviors.same

        case InvalidEbookParams(message) =>
          replyTo ! ValidationFailure(message)
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
            possibleSessionResolution
          }
          else
            Behaviors.same

        case MapFailure =>
          replyTo ! InternalFailure
          Behaviors.stopped

        case _ =>
          Behaviors.unhandled
      }
    }.narrow[NotUsed]
  }
}
