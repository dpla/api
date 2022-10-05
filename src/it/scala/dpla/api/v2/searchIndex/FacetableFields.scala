//package dpla.api.v2.searchIndex
//
//import akka.actor.testkit.typed.scaladsl.ActorTestKit
//import akka.actor.typed.{ActorRef, ActorSystem}
//import akka.http.scaladsl.model._
//import akka.http.scaladsl.server.Route
//import akka.http.scaladsl.testkit.ScalatestRouteTest
//import dpla.api.Routes
//import dpla.api.helpers.Utils.fakeApiKey
//import dpla.api.v2.analytics.AnalyticsClient
//import dpla.api.v2.analytics.AnalyticsClient.AnalyticsClientCommand
//import dpla.api.v2.authentication.AuthProtocol.AuthenticationCommand
//import dpla.api.v2.authentication.{MockAuthenticator, MockPostgresClientSuccess}
//import dpla.api.v2.email.{EmailClient, MockEmailClientSuccess}
//import dpla.api.v2.registry.{ApiKeyRegistryCommand, MockApiKeyRegistry, MockEbookRegistry, MockItemRegistry, SearchRegistryCommand}
//import dpla.api.v2.search.SearchProtocol.SearchCommand
//import dpla.api.v2.search.{DPLAMAPMapper, JsonFieldReader, MockEbookSearch, MockEboookEsClientSuccess, MockItemEsClientSuccess, MockItemSearch}
//import org.scalatest.matchers.should.Matchers
//import org.scalatest.wordspec.AnyWordSpec
//import spray.json._
//
//class FacetableFields extends AnyWordSpec with Matchers with ScalatestRouteTest
//  with JsonFieldReader {
//
//  lazy val testKit: ActorTestKit = ActorTestKit()
//  override def afterAll(): Unit = testKit.shutdownTestKit
//
//  implicit def typedSystem: ActorSystem[Nothing] = testKit.system
//  override def createActorSystem(): akka.actor.ActorSystem =
//    testKit.system.classicSystem
//
//  val analyticsClient: ActorRef[AnalyticsClientCommand] =
//    testKit.spawn(AnalyticsClient())
//  val postgresClient = testKit.spawn(MockPostgresClient())
//  val emailClient: ActorRef[EmailClient.EmailClientCommand] =
//    testKit.spawn(MockEmailClientSuccess())
//  val mapper = testKit.spawn(DPLAMAPMapper())