package dpla.ebookapi.v1.ebooks

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.ebookapi.v1.AnalyticsClient.AnalyticsClientCommand
import dpla.ebookapi.v1.{AnalyticsClient, ParamValidator, PostgresClient}
import dpla.ebookapi.v1.PostgresClient.PostgresClientCommand
import dpla.ebookapi.v1.ebooks.EbookMapper.MapperCommand
import dpla.ebookapi.v1.ebooks.ElasticSearchClient.EsClientCommand
import dpla.ebookapi.v1.ParamValidator.ValidationCommand


/**
 * Handles the control flow for processing an ebooks request from Routes.
 */

object EbookRegistry extends EbookRegistryBehavior {

  override def spawnParamValidator(
                                    context: ActorContext[EbookRegistryCommand]
                                  ): ActorRef[ValidationCommand] =
    context.spawn(ParamValidator(), "ParamValidatorForEbooks")

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
