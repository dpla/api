package dpla.api.v2.registry

import akka.NotUsed
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import dpla.api.v2.analytics.AnalyticsClient.{AnalyticsClientCommand, TrackFetch, TrackSearch}
import dpla.api.v2.authentication.AuthProtocol.{AccountFound, AccountNotFound, AuthenticationCommand, AuthenticationFailure, FindAccountByKey, InvalidApiKey}
import dpla.api.v2.authentication._
import dpla.api.v2.registry.RegistryProtocol.{ForbiddenFailure, InternalFailure, NotFoundFailure, RegistryResponse, ValidationFailure}
import dpla.api.v2.search.SearchProtocol.{DPLADocFetchResult, DPLADocMultiFetchResult, DPLADocSearchResult, Fetch, FetchNotFound, InvalidSearchParams, Search, SearchCommand, SearchFailure}
import dpla.api.v2.search._


final case class SearchResult(result: DPLADocList) extends RegistryResponse
final case class FetchResult(result: SingleDPLADoc) extends RegistryResponse
final case class MultiFetchResult(result: DPLADocList) extends RegistryResponse

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

  def spawnEbookSearch(context: ActorContext[EbookRegistryCommand]):
    ActorRef[SearchCommand]

  def apply(
             authenticator: ActorRef[AuthenticationCommand],
             analyticsClient: ActorRef[AnalyticsClientCommand]
           ): Behavior[EbookRegistryCommand] = {

    Behaviors.setup[EbookRegistryCommand] { context =>

      // Spawn children.
      val ebookSearch: ActorRef[SearchCommand] =
        spawnEbookSearch(context)

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
      var searchResult: Option[DPLADocList] = None
      var searchResponse: Option[RegistryResponse] = None

      // This behavior is invoked if either the API key has been authorized
      // or the ebook search has been complete.
      def possibleSessionResolution: Behavior[AnyRef] =
        (searchResponse, authorizedAccount) match {
          case (Some(response), Some(account)) =>
            // We've received both message replies, time to complete session.
            // Send final result back to Routes.
            replyTo ! response

            // If the search was successful...
            searchResult match {
              case Some(ebookList) =>
                // ...and if account is not staff/internal...
                if (!account.staff.getOrElse(false) && !account.email.endsWith("@dp.la")) {
                  // ...track analytics hit
                  analyticsClient ! TrackSearch(rawParams, host, path,
                    ebookList.docs, "Ebook")
                }
              case None => // no-op
            }
            Behaviors.stopped
          case _ =>
            // Still waiting for one of the message replies.
            Behaviors.same
        }

      // Send initial messages
      authenticator ! FindAccountByKey(apiKey, context.self)
      ebookSearch ! Search(rawParams, context.self)

      Behaviors.receiveMessage {

        /**
         * Possible responses from Authenticator.
         * If authentication fails, no need to wait for EbookSearch to reply to
         * Routes.
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
         * Always check to see if user is authenticated before replying to
         * Routes.
         */

        case DPLADocSearchResult(dplaDocList) =>
          searchResult = Some(dplaDocList)
          searchResponse = Some(SearchResult(dplaDocList))
          possibleSessionResolution

        case InvalidSearchParams(message) =>
          searchResponse = Some(ValidationFailure(message))
          possibleSessionResolution

        case SearchFailure =>
          searchResponse = Some(InternalFailure)
          possibleSessionResolution

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

      var authorizedAccount: Option[Account] = None
      var fetchResult: Option[Either[SingleDPLADoc, DPLADocList]] = None
      var fetchResponse: Option[RegistryResponse] = None

      // This behavior is invoked if either the API key has been authorized
      // or the ebook fetch has been complete.
      def possibleSessionResolution: Behavior[AnyRef] =
        (fetchResponse, authorizedAccount) match {
          case (Some(response), Some(account)) =>
            // We've received both message replies, time to complete session.
            // Send final result back to Routes.
            replyTo ! response

            // If the fetch was successful...
            fetchResult match {
              case Some(either) =>
                // ...and if account is not staff/internal...
                if (!account.staff.getOrElse(false) && !account.email.endsWith("@dp.la")) {
                  // ...track analytics hit.
                  either match {
                    case Left(singleEbook) =>
                      analyticsClient ! TrackFetch(host, path,
                        singleEbook.docs.headOption, "Ebook")
                    case Right(ebookList) =>
                      analyticsClient ! TrackSearch(rawParams, host, path,
                        ebookList.docs, "Ebook")
                  }
                }
              case None => // no-op
            }
            Behaviors.stopped
          case _ =>
            // Still waiting for one of the message replies.
            Behaviors.same
        }

      // Send initial messages.
      authenticator ! FindAccountByKey(apiKey, context.self)
      ebookSearch ! Fetch(id, rawParams, context.self)

      Behaviors.receiveMessage {

        /**
         * Possible responses from Authenticator.
         * If authentication fails, no need to wait for EbookSearch to reply to
         * Routes.
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
         * Always check to see if user is authenticated before replying to
         * Routes.
         */

        case DPLADocFetchResult(singleDPLADoc) =>
          fetchResult = Some(Left(singleDPLADoc))
          fetchResponse = Some(FetchResult(singleDPLADoc))
          possibleSessionResolution

        case DPLADocMultiFetchResult(dplaDocList) =>
          fetchResult = Some(Right(dplaDocList))
          fetchResponse = Some(MultiFetchResult(dplaDocList))
          possibleSessionResolution

        case InvalidSearchParams(message) =>
          fetchResponse = Some(ValidationFailure(message))
          possibleSessionResolution

        case FetchNotFound =>
          fetchResponse = Some(NotFoundFailure)
          possibleSessionResolution

        case SearchFailure =>
          fetchResponse = Some(InternalFailure)
          possibleSessionResolution

        case _ =>
          Behaviors.unhandled
      }
    }.narrow[NotUsed]
  }
}