package dpla.ebookapi.mocks

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.ebookapi.v1.AnalyticsClient.AnalyticsClientCommand
import dpla.ebookapi.v1.AnalyticsClient
import dpla.ebookapi.v1.authentication.{Authenticator, AuthenticatorCommand}
import dpla.ebookapi.v1.registry.{EbookRegistryBehavior, EbookRegistryCommand}
import dpla.ebookapi.v1.search.SearchProtocol.SearchCommand
import dpla.ebookapi.v1.search.EbookSearch

class MockEbookRegistry(testKit: ActorTestKit) {

  private var authenticator: Option[ActorRef[AuthenticatorCommand]] = None
  private var ebookSearch: Option[ActorRef[SearchCommand]] = None
  private var analyticsClient: Option[ActorRef[AnalyticsClientCommand]] = None

  def setAuthenticator(ref: ActorRef[AuthenticatorCommand]): Unit =
    authenticator = Some(ref)

  def setEbookSearch(ref: ActorRef[SearchCommand]): Unit =
    ebookSearch = Some(ref)

  def setAnalyticsClient(ref: ActorRef[AnalyticsClientCommand]): Unit =
    analyticsClient = Some(ref)

  object Mock extends EbookRegistryBehavior {

    override def spawnAuthenticator(
                                     context: ActorContext[EbookRegistryCommand]
                                   ): ActorRef[AuthenticatorCommand] =
      authenticator.getOrElse(context.spawnAnonymous(Authenticator()))

    override def spawnEbookSearch(
                                   context: ActorContext[EbookRegistryCommand]
                                 ): ActorRef[SearchCommand] =
      ebookSearch.getOrElse(context.spawnAnonymous(EbookSearch()))

    override def spawnAnalyticsClient(
                                       context: ActorContext[EbookRegistryCommand]
                                     ): ActorRef[AnalyticsClientCommand] =
      analyticsClient.getOrElse(context.spawnAnonymous(AnalyticsClient()))
  }

  def getRef: ActorRef[EbookRegistryCommand] = testKit.spawn(Mock())
}
