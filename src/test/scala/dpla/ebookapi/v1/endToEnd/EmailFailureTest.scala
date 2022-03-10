package dpla.ebookapi.v1.endToEnd

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.ebookapi.Routes
import dpla.ebookapi.v1.email.EmailClient.EmailClientCommand
import dpla.ebookapi.v1.authentication.AuthProtocol.AuthenticationCommand
import dpla.ebookapi.v1.authentication.{MockAuthenticator, MockPostgresClientSuccess}
import dpla.ebookapi.v1.email.MockEmailClientFailure
import dpla.ebookapi.v1.registry.{ApiKeyRegistryCommand, EbookRegistryCommand, MockApiKeyRegistry, MockEbookRegistry}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class EmailFailureTest extends AnyWordSpec with Matchers with ScalatestRouteTest {

  lazy val testKit: ActorTestKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system

  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  val postgresClient = testKit.spawn(MockPostgresClientSuccess())
  val emailClient: ActorRef[EmailClientCommand] =
    testKit.spawn(MockEmailClientFailure())

  val mockAuthenticator = new MockAuthenticator(testKit)
  mockAuthenticator.setPostgresClient(postgresClient)
  val authenticator: ActorRef[AuthenticationCommand] = mockAuthenticator.getRef

  val mockEbookRegistry = new MockEbookRegistry(testKit, authenticator)
  val ebookRegistry: ActorRef[EbookRegistryCommand] =
    mockEbookRegistry.getRef

  val mockApiKeyRegistry = new MockApiKeyRegistry(testKit, authenticator)
  mockApiKeyRegistry.setEmailClient(emailClient)
  val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
    mockApiKeyRegistry.getRef

  lazy val routes: Route =
    new Routes(ebookRegistry, apiKeyRegistry).applicationRoutes

  "/api_key/[email]" should {
    "return Teapot if email fails" in {
      val validEmail = "test@example.com"
      val request = Post(s"/v1/api_key/$validEmail")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.ImATeapot
      }
    }
  }
}
