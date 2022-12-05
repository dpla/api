package dpla.api

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.api.helpers.ActorHelper
import dpla.api.v2.authentication.AuthProtocol.AuthenticationCommand
import dpla.api.v2.authentication.MockAuthenticator
import dpla.api.v2.registry.{ApiKeyRegistryCommand, MockApiKeyRegistry, MockEbookRegistry, MockItemRegistry, MockPssRegistry, SearchRegistryCommand}


class HealthCheckTest extends AnyWordSpec with Matchers with ScalatestRouteTest
  with ActorHelper {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit()

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  val authenticator: ActorRef[AuthenticationCommand] =
    MockAuthenticator(testKit)

  val ebookRegistry: ActorRef[SearchRegistryCommand] =
    MockEbookRegistry(testKit, authenticator, ebookAnalyticsClient)

  val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
    MockApiKeyRegistry(testKit, authenticator)

  val itemRegistry: ActorRef[SearchRegistryCommand] =
    MockItemRegistry(testKit, authenticator, itemAnalyticsClient)

  val pssRegistry: ActorRef[SearchRegistryCommand] =
    MockPssRegistry(testKit, authenticator, pssAnalyticsClient)

  lazy val routes: Route =
    new Routes(ebookRegistry, itemRegistry, pssRegistry, apiKeyRegistry)
      .applicationRoutes

  "Health check" should {
    "return OK" in {
      val request = Get("/health-check")

      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }
  }
}
