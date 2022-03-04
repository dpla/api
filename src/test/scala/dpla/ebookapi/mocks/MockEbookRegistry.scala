package dpla.ebookapi.mocks

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.ebookapi.v1.AnalyticsClient.AnalyticsClientCommand
import dpla.ebookapi.v1.ebooks.EbookParamValidator.EbookValidationCommand
import dpla.ebookapi.v1.PostgresClient.PostgresClientCommand
import dpla.ebookapi.v1.{AnalyticsClient, PostgresClient}
import dpla.ebookapi.v1.ebooks.EbookMapper.MapperCommand
import dpla.ebookapi.v1.ebooks.ElasticSearchClient.EsClientCommand
import dpla.ebookapi.v1.ebooks.{EbookMapper, EbookParamValidator, EbookRegistryBehavior, EbookRegistryCommand, ElasticSearchClient}

class MockEbookRegistry(testKit: ActorTestKit) {

  private var paramValidator: Option[ActorRef[EbookValidationCommand]] = None
  private var authenticationClient: Option[ActorRef[PostgresClientCommand]] = None
  private var searchIndexClient: Option[ActorRef[EsClientCommand]] = None
  private var ebookMapper: Option[ActorRef[MapperCommand]] = None
  private var analyticsClient: Option[ActorRef[AnalyticsClientCommand]] = None

  def setParmaValidator(ref: ActorRef[EbookValidationCommand]): Unit =
    paramValidator = Some(ref)

  def setAuthenticationClient(ref: ActorRef[PostgresClientCommand]): Unit =
    authenticationClient = Some(ref)

  def setSearchIndexClient(ref: ActorRef[EsClientCommand]): Unit =
    searchIndexClient = Some(ref)

  def setEbookMapper(ref: ActorRef[MapperCommand]): Unit =
    ebookMapper = Some(ref)

  def setAnalyticsClient(ref: ActorRef[AnalyticsClientCommand]): Unit =
    analyticsClient = Some(ref)

  object Mock extends EbookRegistryBehavior {

    override def spawnParamValidator(
                                      context: ActorContext[EbookRegistryCommand]
                                    ): ActorRef[EbookValidationCommand] =
      paramValidator.getOrElse(context.spawnAnonymous(EbookParamValidator()))

    override def spawnAuthenticationClient(
                                            context: ActorContext[EbookRegistryCommand]
                                          ): ActorRef[PostgresClientCommand] =
      authenticationClient.getOrElse(context.spawnAnonymous(PostgresClient()))

    override def spawnSearchIndexClient(
                                         context: ActorContext[EbookRegistryCommand]
                                       ): ActorRef[EsClientCommand] =
      searchIndexClient.getOrElse(context.spawnAnonymous(ElasticSearchClient()))

    override def spawnEbookMapper(
                                   context: ActorContext[EbookRegistryCommand]
                                 ): ActorRef[MapperCommand] =
      ebookMapper.getOrElse(context.spawnAnonymous(EbookMapper()))

    override def spawnAnalyticsClient(
                                       context: ActorContext[EbookRegistryCommand]
                                     ): ActorRef[AnalyticsClientCommand] =
      analyticsClient.getOrElse(context.spawnAnonymous(AnalyticsClient()))

  }

  def getRef: ActorRef[EbookRegistryCommand] = testKit.spawn(Mock())
}
