package dpla.ebookapi.v1.ebooks

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.ebookapi.v1.AnalyticsClient.AnalyticsClientCommand
import dpla.ebookapi.v1.AnalyticsClient
import dpla.ebookapi.v1.authentication.PostgresClient.PostgresClientCommand
import dpla.ebookapi.v1.ebooks.EbookMapper.MapperCommand
import dpla.ebookapi.v1.ebooks.ElasticSearchClient.EsClientCommand
import EbookParamValidator.EbookValidationCommand
import dpla.ebookapi.v1.authentication.PostgresClient


/**
 * Handles the control flow for processing an ebooks request from Routes.
 */

object EbookRegistry extends EbookRegistryBehavior {

  override def spawnParamValidator(
                                    context: ActorContext[EbookRegistryCommand]
                                  ): ActorRef[EbookValidationCommand] =
    context.spawn(EbookParamValidator(), "EbookParamValidator")

  override def spawnAuthenticationClient(
                                          context: ActorContext[EbookRegistryCommand]
                                        ): ActorRef[PostgresClientCommand] =
    context.spawn(PostgresClient(), "AuthenticationClientForEbooks")

  override def spawnSearchIndexClient(
                                       context: ActorContext[EbookRegistryCommand]
                                     ): ActorRef[EsClientCommand] =
    context.spawn(ElasticSearchClient(), "SearchIndexClientForEbooks")

  override def spawnEbookMapper(
                                 context: ActorContext[EbookRegistryCommand]
                               ): ActorRef[MapperCommand] =
    context.spawn(EbookMapper(), "EbookMapperForEbooks")

  override def spawnAnalyticsClient(
                                     context: ActorContext[EbookRegistryCommand]
                                   ): ActorRef[AnalyticsClientCommand] =
    context.spawn(AnalyticsClient(), "AnalyticsClientForEbooks")
}
