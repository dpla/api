package dpla.api.v2.endToEnd

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.api.Routes
import dpla.api.helpers.ActorHelper
import dpla.api.helpers.Utils.fakeApiKey
import dpla.api.v2.registry.{ApiKeyRegistryCommand, MockApiKeyRegistry}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PostgresErrorTest extends AnyWordSpec with Matchers
  with ScalatestRouteTest with ActorHelper {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  val apiKeyRegistryAuthError: ActorRef[ApiKeyRegistryCommand] =
    MockApiKeyRegistry(testKit, authenticatorError)

  val apiKeyRegistryExistingKey: ActorRef[ApiKeyRegistryCommand] =
    MockApiKeyRegistry(testKit, authenticatorExistingKey, Some(emailClient))

  "/api_key/[email]" should {
    "return InternalServerError if Postgres errors" in {
      lazy val routes: Route =
        new Routes(itemRegistry, pssRegistry,
          apiKeyRegistryAuthError, smrRegistry).applicationRoutes

      val request = Post(s"/v2/api_key/email@example.com")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.InternalServerError
        contentType should === (ContentTypes.`application/json`)
      }
    }
  }

  "/api_key/[email] route" should {
    "return Conflict if email has existing api key" in {
      lazy val routes: Route =
        new Routes(itemRegistry, pssRegistry,
          apiKeyRegistryExistingKey, smrRegistry).applicationRoutes

      val request = Post("/v2/api_key/email@example.com")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.Conflict
        contentType should === (ContentTypes.`application/json`)
      }
    }
  }
}
