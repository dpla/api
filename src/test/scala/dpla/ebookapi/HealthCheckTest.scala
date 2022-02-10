package dpla.ebookapi

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.ebookapi.mocks.MockEsClientSuccess
import dpla.ebookapi.v1.ebooks.{EbookRegistry, ElasticSearchClient}

class HealthCheckTest extends AnyWordSpec with Matchers with ScalatestRouteTest {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit()

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem
  val ebookRegistry: ActorRef[EbookRegistry.RegistryCommand] =
    testKit.spawn(EbookRegistry())
  val elasticSearchClient: ActorRef[ElasticSearchClient.EsClientCommand] =
    testKit.spawn(MockEsClientSuccess())
  lazy val routes: Route =
    new Routes(ebookRegistry, elasticSearchClient).applicationRoutes

  "Health check" should {
    "return OK" in {
      val request = Get("/health-check")

      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }
  }
}
