package dpla.ebookapi.v1.ebooks.endToEnd

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.ebookapi.Routes
import dpla.ebookapi.mocks.MockEsClientSuccess
import dpla.ebookapi.v1.ebooks.{EbookRegistry, JsonFieldReader}
import dpla.ebookapi.v1.ebooks.ElasticSearchClient.EsClientCommand
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json._

class HappyPathsTest extends AnyWordSpec with Matchers with ScalatestRouteTest with JsonFieldReader {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem = testKit.system.classicSystem
  val ebookRegistry: ActorRef[EbookRegistry.RegistryCommand] = testKit.spawn(EbookRegistry())
  val elasticSearchClient: ActorRef[EsClientCommand] = testKit.spawn(MockEsClientSuccess())
  lazy val routes: Route = new Routes(ebookRegistry, elasticSearchClient).applicationRoutes

  "/v1/ebooks route" should {
    "be happy with valid user inputs and successful es response" in {
      val request = Get("/v1/ebooks?page_size=100")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.OK
        contentType should === (ContentTypes.`application/json`)

        val entity: JsObject = entityAs[String].parseJson.asJsObject

        val limit: Option[Int] = readInt(entity, "limit")
        limit should === (Some(100))

        val expected = Seq(
          "ufwPJ34Bj-MaVWqX9KZL",
          "uvwPJ34Bj-MaVWqX9KZL",
          "u_wPJ34Bj-MaVWqX9KZZ",
          "vPwPJ34Bj-MaVWqX9KZZ",
          "vfwPJ34Bj-MaVWqX9KZZ",
          "vvwPJ34Bj-MaVWqX9KZZ",
          "v_wPJ34Bj-MaVWqX9Kac",
          "wPwPJ34Bj-MaVWqX9Kac",
          "wfwPJ34Bj-MaVWqX9Kac",
          "wvwPJ34Bj-MaVWqX9Kac"
        )
        val ids = readObjectArray(entity, "docs").flatMap(readString(_, "id"))
        ids should contain allElementsOf expected
      }
    }
  }

  "/v1/ebooks/[id] route" should {
    "be happy with valid user inputs and successful es response" in {
      val request = Get("/v1/ebooks/wfwPJ34Bj-MaVWqX9Kac")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.OK
        contentType should ===(ContentTypes.`application/json`)

        val entity: JsObject = entityAs[String].parseJson.asJsObject
        val id = readObjectArray(entity, "docs").flatMap(readString(_, "id")).headOption
        id should === (Some("wfwPJ34Bj-MaVWqX9Kac"))
      }
    }
  }
}
