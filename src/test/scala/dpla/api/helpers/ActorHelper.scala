package dpla.api.helpers

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.api.Routes
import dpla.api.v2.analytics.{AnalyticsClientCommand, EbookAnalyticsClient, ItemAnalyticsClient, PssAnalyticsClient}
import dpla.api.v2.authentication.AuthProtocol.AuthenticationCommand
import dpla.api.v2.authentication._
import dpla.api.v2.email.EmailClient.EmailClientCommand
import dpla.api.v2.email.{MockEmailClientFailure, MockEmailClientSuccess}
import dpla.api.v2.registry.{ApiKeyRegistryCommand, MockApiKeyRegistry, MockEbookRegistry, MockItemRegistry, MockPssRegistry, SearchRegistryCommand}
import dpla.api.v2.search.SearchProtocol.SearchCommand
import dpla.api.v2.search.{MockEbookSearch, MockEboookEsClientSuccess, MockEsClientFailure, MockEsClientNotFound, SearchProtocol}
import dpla.api.v2.search.mappings.{DPLAMAPMapper, MockMapperFailure}

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

  val dplaMapMapper = testKit.spawn(DPLAMAPMapper())

  val mapperFailure = testKit.spawn(MockMapperFailure())

  val ebookElasticSearchClient = testKit.spawn(MockEboookEsClientSuccess(dplaMapMapper))

  val elasticSearchClientFailure = testKit.spawn(MockEsClientFailure())

  val elasticSearchClientNotFound = testKit.spawn(MockEsClientNotFound())

////  implicit def typedSystem: ActorSystem[Nothing] = testKit.system
////  override def createActorSystem(): akka.actor.ActorSystem =
////    testKit.system.classicSystem
//
//  val defaultPostgresClient =
//    testKit.spawn(MockPostgresClientSuccess())
//
//  val defaultEmailClient: ActorRef[EmailClientCommand] =
//    testKit.spawn(MockEmailClientSuccess())
//
//  val defaultAuthenticator: ActorRef[AuthenticationCommand] =
//    MockAuthenticator(testKit, Some(defaultPostgresClient))
//
//  val defaultEbookAnalyticsClient: ActorRef[AnalyticsClientCommand] =
//    testKit.spawn(EbookAnalyticsClient())
//
//  val defaultItemAnalyticsClient: ActorRef[AnalyticsClientCommand] =
//    testKit.spawn(ItemAnalyticsClient())
//
//  val defaultPssAnalyticsClient: ActorRef[AnalyticsClientCommand] =
//    testKit.spawn(PssAnalyticsClient())
//
//  val defaultEbookRegistry: ActorRef[SearchRegistryCommand] =
//    MockEbookRegistry(testKit, defaultAuthenticator, defaultEbookAnalyticsClient)
//
//  val defaultApiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
//    MockApiKeyRegistry(testKit, defaultPostgresClient, Some(defaultEmailClient))
//
//  val defaultItemRegistry: ActorRef[SearchRegistryCommand] =
//    MockItemRegistry(testKit, defaultAuthenticator, defaultItemAnalyticsClient)
//
//  val defaultPssRegistry: ActorRef[SearchRegistryCommand] =
//    MockPssRegistry(testKit, defaultAuthenticator, defaultPssAnalyticsClient)
//
//  lazy val defaultRoutes: Route =
//    new Routes(defaultEbookRegistry, defaultItemRegistry, defaultPssRegistry, defaultApiKeyRegistry)
//      .applicationRoutes
}