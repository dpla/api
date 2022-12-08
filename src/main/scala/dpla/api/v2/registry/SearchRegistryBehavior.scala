package dpla.api.v2.registry

import akka.NotUsed
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import dpla.api.v2.analytics.{AnalyticsClientCommand, TrackSearch}
import dpla.api.v2.authentication.AuthProtocol._
import dpla.api.v2.authentication._
import dpla.api.v2.registry.RegistryProtocol._
import dpla.api.v2.search.SearchProtocol._
import dpla.api.v2.search.mappings.MappedResponse


final case class SearchResult(result: MappedResponse) extends RegistryResponse

sealed trait SearchRegistryCommand

final case class RegisterSearch(
                                 apiKey: Option[String] = None,
                                 rawParams: Map[String, String],
                                 host: String,
                                 path: String,
                                 replyTo: ActorRef[RegistryResponse]
                               ) extends SearchRegistryCommand

final case class RegisterFetch(
                                apiKey: Option[String] = None,
                                id: String,
                                rawParams: Map[String, String],
                                host: String,
                                path: String,
                                replyTo: ActorRef[RegistryResponse]
                              ) extends SearchRegistryCommand

final case class RegisterRandom(
                                 apiKey: Option[String] = None,
                                 rawParams: Map[String, String],
                                 replyTo: ActorRef[RegistryResponse]
                               ) extends SearchRegistryCommand

trait SearchRegistryBehavior {

  def spawnSearchActor(context: ActorContext[SearchRegistryCommand]):
    ActorRef[SearchCommand]

  def apply(
             authenticator: ActorRef[AuthenticationCommand],
             analyticsClient: ActorRef[AnalyticsClientCommand]
           ): Behavior[SearchRegistryCommand] = {

    Behaviors.setup[SearchRegistryCommand] { context =>

      // Spawn children.
      val searchActor: ActorRef[SearchCommand] =
        spawnSearchActor(context)

      Behaviors.receiveMessage[SearchRegistryCommand] {

        case RegisterSearch(apiKey, rawParams, host, path, replyTo) =>
          // Create a session child actor to process the request.
          val sessionChildActor =
            processSearch(apiKey, rawParams, host, path, replyTo, authenticator,
              searchActor, analyticsClient)
          context.spawnAnonymous(sessionChildActor)
          Behaviors.same

        case RegisterFetch(apiKey, id, rawParams, host, path, replyTo) =>
          // Create a session child actor to process the request.
          val sessionChildActor =
            processFetch(apiKey, id, rawParams, host, path, replyTo,
              authenticator, searchActor, analyticsClient)
          context.spawnAnonymous(sessionChildActor)
          Behaviors.same

        case RegisterRandom(apiKey, rawParams, replyTo) =>
          // Create a session child actor to process the request.
          val sessionChildActor =
            processRandom(apiKey, rawParams, replyTo,
              authenticator, searchActor)
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
                             searchActor: ActorRef[SearchCommand],
                             analyticsClient: ActorRef[AnalyticsClientCommand]
                           ): Behavior[NotUsed] = {

    Behaviors.setup[AnyRef] { context =>

      var authorizedAccount: Option[Account] = None
      var searchResult: Option[MappedResponse] = None
      var searchResponse: Option[RegistryResponse] = None

      // This behavior is invoked if either the API key has been authorized
      // or the search has been complete.
      def possibleSessionResolution: Behavior[AnyRef] =
        (searchResponse, authorizedAccount) match {
          case (Some(response), Some(account)) =>
            // We've received both message replies, time to complete session.
            // Send final result back to Routes.
            replyTo ! response

            // If the search was successful...
            searchResult match {
              case Some(mappedResponse) =>
                // ...and if account is not staff/internal...
                if (!account.staff.getOrElse(false) && !account.email.endsWith("@dp.la")) {
                  // ...track analytics hit
                  analyticsClient ! TrackSearch(rawParams, host, path, mappedResponse)
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
      searchActor ! Search(rawParams, context.self)

      Behaviors.receiveMessage {

        /**
         * Possible responses from Authenticator.
         * If authentication fails, no need to wait for search actor to reply to
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
         * Possible responses from search actor.
         * Always check to see if user is authenticated before replying to
         * Routes.
         */

        case MappedSearchResult(mappedResponse) =>
          searchResult = Some(mappedResponse)
          searchResponse = Some(SearchResult(mappedResponse))
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
                            searchActor: ActorRef[SearchCommand],
                            analyticsClient: ActorRef[AnalyticsClientCommand]
                          ): Behavior[NotUsed] = {

    Behaviors.setup[AnyRef] { context =>

      var authorizedAccount: Option[Account] = None
      var fetchResult: Option[MappedResponse] = None
      var fetchResponse: Option[RegistryResponse] = None

      // This behavior is invoked if either the API key has been authorized
      // or the fetch has been complete.
      def possibleSessionResolution: Behavior[AnyRef] =
        (fetchResponse, authorizedAccount) match {
          case (Some(response), Some(account)) =>
            // We've received both message replies, time to complete session.
            // Send final result back to Routes.
            replyTo ! response

            // If the fetch was successful...
            fetchResult match {
              case Some(mapped) =>
                // ...and if account is not staff/internal...
                if (!account.staff.getOrElse(false) && !account.email.endsWith("@dp.la"))
                  // ...track analytics hit.
                  analyticsClient ! TrackSearch(rawParams, host, path, mapped)
              case None => // no-op
            }
            Behaviors.stopped
          case _ =>
            // Still waiting for one of the message replies.
            Behaviors.same
        }

      // Send initial messages.
      authenticator ! FindAccountByKey(apiKey, context.self)
      searchActor ! Fetch(id, rawParams, context.self)

      Behaviors.receiveMessage {

        /**
         * Possible responses from Authenticator.
         * If authentication fails, no need to wait for search actor to reply to
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
         * Possible responses from search actor.
         * Always check to see if user is authenticated before replying to
         * Routes.
         */

        case MappedFetchResult(mappedResponse) =>
          fetchResult = Some(mappedResponse)
          fetchResponse = Some(SearchResult(mappedResponse))
          possibleSessionResolution

        case MappedMultiFetchResult(mappedResponse) =>
          fetchResult = Some(mappedResponse)
          fetchResponse = Some(SearchResult(mappedResponse))
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

  private def processRandom(
                             apiKey: Option[String],
                             rawParams: Map[String, String],
                             replyTo: ActorRef[RegistryResponse],
                             authenticator: ActorRef[AuthenticationCommand],
                             searchActor: ActorRef[SearchCommand],
                           ): Behavior[NotUsed] = {

    Behaviors.setup[AnyRef] { context =>

      var authorizedAccount: Option[Account] = None
      var randomResponse: Option[RegistryResponse] = None

      // This behavior is invoked if either the API key has been authorized
      // or the random fetch has been complete.
      def possibleSessionResolution: Behavior[AnyRef] =
        (randomResponse, authorizedAccount) match {
          case (Some(response), Some(_)) =>
            // We've received both message replies, time to complete session.
            // Send final result back to Routes.
            replyTo ! response
            Behaviors.stopped
          case _ =>
            // Still waiting for one of the message replies.
            Behaviors.same
        }

      // Send initial messages.
      authenticator ! FindAccountByKey(apiKey, context.self)
      searchActor ! Random(rawParams, context.self)

      Behaviors.receiveMessage {

        /**
         * Possible responses from Authenticator.
         * If authentication fails, no need to wait for search actor to reply to
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
         * Possible responses from search actor.
         * Always check to see if user is authenticated before replying to
         * Routes.
         */

        case MappedRandomResult(mappedResponse) =>
          randomResponse = Some(SearchResult(mappedResponse))
          possibleSessionResolution

        case InvalidSearchParams(message) =>
          randomResponse = Some(ValidationFailure(message))
          possibleSessionResolution

        case SearchFailure =>
          randomResponse = Some(InternalFailure)
          possibleSessionResolution

        case _ =>
          Behaviors.unhandled
      }
    }.narrow[NotUsed]
  }
}
