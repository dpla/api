package dpla.ebookapi.v1.ebooks.endToEnd

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.ebookapi.Routes
import dpla.ebookapi.mocks.{MockApiKeyRegistry, MockAuthenticator, MockEbookRegistry, MockEmailClientFailure, MockEsClientSuccess, MockPostgresClientSuccess}
import dpla.ebookapi.v1.EmailClient.EmailClientCommand
import dpla.ebookapi.v1.authentication.PostgresClient.PostgresClientCommand
import dpla.ebookapi.v1.apiKey.ApiKeyRegistryCommand
import dpla.ebookapi.v1.authentication.AuthenticatorCommand
import dpla.ebookapi.v1.ebooks.EbookRegistryCommand
import dpla.ebookapi.v1.ebooks.ElasticSearchClient.EsClientCommand
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class EmailFailureTest extends AnyWordSpec with Matchers with ScalatestRouteTest {

  lazy val testKit: ActorTestKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system

  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  val postgresClient: ActorRef[PostgresClientCommand] =
    testKit.spawn(MockPostgresClientSuccess())
  val elasticSearchClient: ActorRef[EsClientCommand] =
    testKit.spawn(MockEsClientSuccess())
  val emailClient: ActorRef[EmailClientCommand] =
    testKit.spawn(MockEmailClientFailure())

  val mockEbookRegistry = new MockEbookRegistry(testKit)
  val ebookRegistry: ActorRef[EbookRegistryCommand] =
    mockEbookRegistry.getRef

  val mockAuthenticator = new MockAuthenticator(testKit)
  mockAuthenticator.setPostgresClient(postgresClient)
  val authenticator: ActorRef[AuthenticatorCommand] = mockAuthenticator.getRef

  val mockApiKeyRegistry = new MockApiKeyRegistry(testKit)
  mockApiKeyRegistry.setAuthenticator(authenticator)
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
