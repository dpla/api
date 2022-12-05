package dpla.api.helpers

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.api.Routes
import dpla.api.v2.analytics.{AnalyticsClientCommand, EbookAnalyticsClient, ItemAnalyticsClient, PssAnalyticsClient}
import dpla.api.v2.authentication.AuthProtocol.AuthenticationCommand
import dpla.api.v2.authentication.{MockAuthenticator, MockPostgresClientSuccess}
import dpla.api.v2.email.EmailClient.EmailClientCommand
import dpla.api.v2.email.{MockEmailClientFailure, MockEmailClientSuccess}
import dpla.api.v2.registry.{ApiKeyRegistryCommand, MockApiKeyRegistry, MockEbookRegistry, MockItemRegistry, MockPssRegistry, SearchRegistryCommand}

trait ActorHelper {

  /** Abstract methods */
  val testKit: ActorTestKit


  val ebookAnalyticsClient: ActorRef[AnalyticsClientCommand] =
  testKit.spawn(EbookAnalyticsClient())

  val itemAnalyticsClient: ActorRef[AnalyticsClientCommand] =
  testKit.spawn(ItemAnalyticsClient())

  val pssAnalyticsClient: ActorRef[AnalyticsClientCommand] =
  testKit.spawn(PssAnalyticsClient())
//

//
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