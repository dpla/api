package dpla.ebookapi.v1.registry

import akka.NotUsed
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import dpla.ebookapi.v1.AnalyticsClient.{AnalyticsClientCommand, TrackFetch, TrackSearch}
import dpla.ebookapi.v1._
import dpla.ebookapi.v1.authentication.AuthProtocol.{AccountFound, AccountNotFound, AuthenticationCommand, AuthenticationFailure, FindAccountByKey, InvalidApiKey}
import dpla.ebookapi.v1.authentication._
import dpla.ebookapi.v1.search.SearchProtocol.{EbookFetchResult, EbookSearchResult, Fetch, FetchNotFound, InvalidSearchParams, Search, SearchCommand, SearchFailure}
import dpla.ebookapi.v1.search._


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
    ActorRef[AuthenticationCommand]

  def spawnEbookSearch(context: ActorContext[EbookRegistryCommand]):
    ActorRef[SearchCommand]

  def spawnAnalyticsClient(context: ActorContext[EbookRegistryCommand]):
    ActorRef[AnalyticsClientCommand]

  def apply(): Behavior[EbookRegistryCommand] = {

    Behaviors.setup[EbookRegistryCommand] { context =>

      // Spawn children.

      val authenticator: ActorRef[AuthenticationCommand] =
        spawnAuthenticator(context)

      val ebookSearch: ActorRef[SearchCommand] =
        spawnEbookSearch(context)

      val analyticsClient: ActorRef[AnalyticsClientCommand] =
        spawnAnalyticsClient(context)

      Behaviors.receiveMessage[EbookRegistryCommand] {

        case SearchEbooks(apiKey, rawParams, host, path, replyTo) =>
          // Create a session child actor to process the request.
          val sessionChildActor =
            processSearch(apiKey, rawParams, host, path, replyTo, authenticator,
              ebookSearch, analyticsClient)
          context.spawnAnonymous(sessionChildActor)
          Behaviors.same

        case FetchEbook(apiKey, id, rawParams, host, path, replyTo) =>
          // Create a session child actor to process the request.
          val sessionChildActor =
            processFetch(apiKey, id, rawParams, host, path, replyTo,
              authenticator, ebookSearch, analyticsClient)
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
                             authenticator: ActorRef[AuthenticationCommand],
                             ebookSearch: ActorRef[SearchCommand],
                             analyticsClient: ActorRef[AnalyticsClientCommand]
                           ): Behavior[NotUsed] = {

    Behaviors.setup[AnyRef] { context =>

      var authorizedAccount: Option[Account] = None
      var searchResult: Option[EbookList] = None

      // TODO always wait for authorization response
      // This behavior is invoked if either the API key has been authorized
      // or the ebook search has been complete.
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
      authenticator ! FindAccountByKey(apiKey.getOrElse(""), context.self)
      ebookSearch ! Search(rawParams, context.self)

      Behaviors.receiveMessage {

        /**
         * Possible responses from Authenticator.
         */

        case AccountFound(account) =>
          if (account.enabled.getOrElse(false)) {
            authorizedAccount = Some(account)
            possibleSessionResolution
          }
          else {
            replyTo ! ForbiddenFailure
            Behaviors.stopped
          }

        case AccountNotFound =>
          replyTo ! ForbiddenFailure
          Behaviors.stopped

        case InvalidApiKey =>
          replyTo ! ForbiddenFailure
          Behaviors.stopped

        case AuthenticationFailure =>
          replyTo ! InternalFailure
          Behaviors.stopped

        /**
         * Possible responses from EbookSearch.
         */

        case EbookSearchResult(ebookList) =>
          if (searchResult.isEmpty) {
            searchResult = Some(ebookList)
            possibleSessionResolution
          }
          else {
            Behaviors.same
          }

        case InvalidSearchParams(message) =>
          replyTo ! ValidationFailure(message)
          Behaviors.stopped

        case SearchFailure =>
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
                            authenticator: ActorRef[AuthenticationCommand],
                            ebookSearch: ActorRef[SearchCommand],
                            analyticsClient: ActorRef[AnalyticsClientCommand]
                          ): Behavior[NotUsed] = {

    Behaviors.setup[AnyRef] { context =>

      var fetchResult: Option[SingleEbook] = None
      var authorizedAccount: Option[Account] = None

      // This behavior is invoked if either the API key has been authorized
      // or the ebook fetch has been complete.
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
      authenticator ! FindAccountByKey(apiKey.getOrElse(""), context.self)
      ebookSearch ! Fetch(id, rawParams, context.self)

      Behaviors.receiveMessage {

        /**
         * Possible responses from Authenticator.
         */

        case AccountFound(account) =>
          if (account.enabled.getOrElse(false)) {
            authorizedAccount = Some(account)
            possibleSessionResolution
          }
          else {
            replyTo ! ForbiddenFailure
            Behaviors.stopped
          }

        case AccountNotFound =>
          replyTo ! ForbiddenFailure
          Behaviors.stopped

        case InvalidApiKey =>
          replyTo ! ForbiddenFailure
          Behaviors.stopped

        case AuthenticationFailure =>
          replyTo ! InternalFailure
          Behaviors.stopped

        /**
         * Possible responses from EbookSearch
         */

        case EbookFetchResult(singleEbook) =>
          if (fetchResult.isEmpty) {
            fetchResult = Some(singleEbook)
            possibleSessionResolution
          }
          else
            Behaviors.same

        case InvalidSearchParams(message) =>
          replyTo ! ValidationFailure(message)
          Behaviors.stopped

        case FetchNotFound =>
          replyTo ! NotFoundFailure
          Behaviors.stopped

        case SearchFailure =>
          replyTo ! InternalFailure
          Behaviors.stopped

        case _ =>
          Behaviors.unhandled
      }
    }.narrow[NotUsed]
  }
}
