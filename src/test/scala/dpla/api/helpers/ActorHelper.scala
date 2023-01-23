package dpla.api.helpers

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import dpla.api.v2.analytics.{AnalyticsClientCommand, EbookAnalyticsClient, ItemAnalyticsClient, PssAnalyticsClient}
import dpla.api.v2.authentication.AuthProtocol.AuthenticationCommand
import dpla.api.v2.authentication._
import dpla.api.v2.email.EmailClient.EmailClientCommand
import dpla.api.v2.email.{MockEmailClientFailure, MockEmailClientSuccess}
import dpla.api.v2.registry.{ApiKeyRegistryCommand, MockApiKeyRegistry, MockItemRegistry, MockPssRegistry, MockSmrRegistry, SearchRegistryCommand, SmrRegistryCommand}
import dpla.api.v2.search.{MockEboookEsClientSuccess, MockEsClientFailure, MockEsClientNotFound, MockItemEsClientSuccess}
import dpla.api.v2.search.mappings.{DPLAMAPMapper, MockMapperFailure}
import dpla.api.v2.smr.{MockS3ClientFailure, MockS3ClientSuccess}

trait ActorHelper {

  /** Abstract methods */
  val testKit: ActorTestKit


  val ebookAnalyticsClient: ActorRef[AnalyticsClientCommand] =
    testKit.spawn(EbookAnalyticsClient())

  val itemAnalyticsClient: ActorRef[AnalyticsClientCommand] =
    testKit.spawn(ItemAnalyticsClient())

  val pssAnalyticsClient: ActorRef[AnalyticsClientCommand] =
    testKit.spawn(PssAnalyticsClient())

  val emailClient: ActorRef[EmailClientCommand] =
    testKit.spawn(MockEmailClientSuccess())

  val emailClientFailure: ActorRef[EmailClientCommand] =
    testKit.spawn(MockEmailClientFailure())

  val authenticator: ActorRef[AuthenticationCommand] =
    MockAuthenticatorSuccess(testKit)

  val authenticatorDisabled: ActorRef[AuthenticationCommand] =
    MockAuthenticatorDisabled(testKit)

  val authenticatorError: ActorRef[AuthenticationCommand] =
    MockAuthenticatorError(testKit)

  val authenticatorExistingKey: ActorRef[AuthenticationCommand] =
    MockAuthenticatorExistingKey(testKit)

  val authenticatorKeyNotFound: ActorRef[AuthenticationCommand] =
    MockAuthenticatorKeyNotFound(testKit)

  val authenticatorStaff: ActorRef[AuthenticationCommand] =
    MockAuthenticatorStaff(testKit)

  val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
    MockApiKeyRegistry(testKit, authenticator)

  val itemRegistry: ActorRef[SearchRegistryCommand] =
    MockItemRegistry(testKit, authenticator, itemAnalyticsClient)

  val pssRegistry: ActorRef[SearchRegistryCommand] =
    MockPssRegistry(testKit, authenticator, pssAnalyticsClient)

  val smrRegistry: ActorRef[SmrRegistryCommand] =
    MockSmrRegistry(testKit, authenticator)

  val dplaMapMapper = testKit.spawn(DPLAMAPMapper())

  val mapperFailure = testKit.spawn(MockMapperFailure())

  val ebookElasticSearchClient = testKit.spawn(MockEboookEsClientSuccess(dplaMapMapper))

  val itemElasticSearchClient = testKit.spawn(MockItemEsClientSuccess(dplaMapMapper))

  val elasticSearchClientFailure = testKit.spawn(MockEsClientFailure())

  val elasticSearchClientNotFound = testKit.spawn(MockEsClientNotFound())

  val s3ClientSuccess = testKit.spawn(MockS3ClientSuccess())

  val s3ClientFailure = testKit.spawn(MockS3ClientFailure())
}
