package dpla.ebookapi.v1.ebooks

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.ebookapi.Routes
import dpla.ebookapi.helpers.Mocks
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.Random

class ParamValidationTest extends AnyWordSpec with Matchers with ScalatestRouteTest with Mocks {

  lazy val testKit: ActorTestKit = ActorTestKit()
  implicit def typedSystem: ActorSystem[Nothing] = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem = testKit.system.classicSystem

  val elasticSearchClient: MockElasticSearchClient = getMockElasticSearchClient
  lazy val routes: Route = new Routes(elasticSearchClient).applicationRoutes

  "search param validator" should {
    "reject unrecognized params" in {
      val request = Get("/v1/ebooks?foo=bar")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }

  "fetch param validator" should {
    "reject unrecognized params" in {
      val request = Get("/v1/ebooks/R0VfVX4BfY91SSpFGqxt?foo=bar")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }

  "ebook ID validator" should {
    "accept valid ID" in {
      val given = "ufwPJ34Bj-MaVWqX9KZL"
      val request = Get(s"/v1/ebooks/$given")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastId shouldEqual given
      }
    }

    "reject ID with special characters" in {
      val given = "<foo>"
      val request = Get(s"/v1/ebooks/$given")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }

    "reject too-long ID" in {
      val given = "asdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdf"
      val request = Get(s"/v1/ebooks/$given")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }

  "creator validator" should {
    "handle empty param" in {
      val expected = None
      val request = Get("/v1/ebooks")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.filters.find(_.fieldName == "sourceResource.creator") shouldEqual expected
      }
    }

    "accept valid param" in {
      val given = "Jules%20Verne"
      val expected = Some("Jules Verne")
      val request = Get(s"/v1/ebooks?sourceResource.creator=$given")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.filters.find(_.fieldName == "sourceResource.creator")
          .map(_.value) shouldEqual expected
      }
    }

    "reject too-short param" in {
      val given = "d"
      val request = Get(s"/v1/ebooks?sourceResource.creator=$given")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val request = Get(s"/v1/ebooks?sourceResource.creator=$given")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }

  "dataProvider validator" should {
    "handle empty param" in {
      val expected = None
      val request = Get("/v1/ebooks")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.filters.find(_.fieldName == "dataProvider") shouldEqual expected
      }
    }

    "accept valid param" in {
      val given = "https://standardebooks.org"
      val expected = Some("https://standardebooks.org")
      val request = Get(s"/v1/ebooks?dataProvider=$given")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.filters.find(_.fieldName == "dataProvider")
          .map(_.value) shouldEqual expected
      }
    }

    "reject invalid URL" in {
      val given = "standardebooks"
      val request = Get(s"/v1/ebooks?dataProvider=$given")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }

  "date validator" should {
    "handle empty param" in {
      val expected = None
      val request = Get("/v1/ebooks")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.filters
          .find(_.fieldName == "sourceResource.date.displayDate") shouldEqual expected
      }
    }

    "accept valid param" in {
      val given = "2002"
      val expected = Some("2002")
      val request = Get(s"/v1/ebooks?sourceResource.date.displayDate=$given")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.filters.find(_.fieldName == "sourceResource.date.displayDate")
          .map(_.value) shouldEqual expected
      }
    }

    "reject too-short param" in {
      val given = "1"
      val request = Get(s"/v1/ebooks?sourceResource.date.displayDate=$given")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val request = Get(s"/v1/ebooks?sourceResource.date.displayDate=$given")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }

  "description validator" should {
    "handle empty param" in {
      val expected = None
      val request = Get("/v1/ebooks")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.filters.find(_.fieldName == "sourceResource.description") shouldEqual expected
      }
    }

    "accept valid param" in {
      val given = "dogs"
      val expected = Some("dogs")
      val request = Get(s"/v1/ebooks?sourceResource.description=$given")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.filters.find(_.fieldName == "sourceResource.description")
          .map(_.value) shouldEqual expected
      }
    }

    "reject too-short param" in {
      val given = "d"
      val request = Get(s"/v1/ebooks?sourceResource.description=$given")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val request = Get(s"/v1/ebooks?sourceResource.description=$given")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }

  "exact_field_match validator" should {
    "handle empty param" in {
      val expected = false
      val request = Get("/v1/ebooks")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.exactFieldMatch shouldEqual expected
      }
    }

    "accept valid param" in {
      val given = "true"
      val expected = true
      val request = Get(s"/v1/ebooks?exact_field_match=$given")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.exactFieldMatch shouldEqual expected
      }
    }

    "reject non-boolean param" in {
      val given = "foo"
      val request = Get(s"/v1/ebooks?exact_field_match=$given")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }

  "facet validator" should {
    "handle empty param" in {
      val expected = None
      val request = Get("/v1/ebooks")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.facets shouldEqual expected
      }
    }

    "accept valid param" in {
      val given = "dataProvider,sourceResource.creator"
      val expected = Some(Seq("dataProvider", "sourceResource.creator"))
      val request = Get(s"/v1/ebooks?facets=$given")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.facets shouldEqual expected
      }
    }

    "reject unfacetable field" in {
      val given = "sourceResource.description"
      val request = Get(s"/v1/ebooks?facets=$given")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }

  "facet size validator" should {
    "handle empty param" in {
      val expected = 50
      val request = Get("/v1/ebooks")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.facetSize shouldEqual expected
      }
    }

    "accept valid param" in {
      val given = "30"
      val expected = 30
      val request = Get(s"/v1/ebooks?facet_size=$given")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.facetSize shouldEqual expected
      }
    }

    "reject non-int param" in {
      val given = "foo"
      val request = Get(s"/v1/ebooks?facet_size=$given")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }

    "reject out-of-range param" in {
      val given = "9999"
      val request = Get(s"/v1/ebooks?facet_size=$given")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }

  "format validator" should {
    "handle empty param" in {
      val expected = None
      val request = Get("/v1/ebooks")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.filters.find(_.fieldName == "sourceResource.format") shouldEqual expected
      }
    }

    "accept valid param" in {
      val given = "article"
      val expected = Some("article")
      val request = Get(s"/v1/ebooks?sourceResource.format=$given")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.filters.find(_.fieldName == "sourceResource.format")
          .map(_.value) shouldEqual expected
      }
    }

    "reject too-short param" in {
      val given = "d"
      val request = Get(s"/v1/ebooks?sourceResource.format=$given")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val request = Get(s"/v1/ebooks?sourceResource.format=$given")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }

  "isShownAt validator" should {
    "handle empty param" in {
      val expected = None
      val request = Get("/v1/ebooks")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.filters.find(_.fieldName == "isShownAt") shouldEqual expected
      }
    }

    "accept valid param" in {
      val given = "\"https://standardebooks.org/ebooks/j-s-fletcher/the-charing-cross-mystery\""
      val expected = Some("\"https://standardebooks.org/ebooks/j-s-fletcher/the-charing-cross-mystery\"")
      val request = Get(s"/v1/ebooks?isShownAt=$given")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.filters.find(_.fieldName == "isShownAt")
          .map(_.value) shouldEqual expected
      }
    }

    "reject invalid URL" in {
      val given = "the-charing-cross-mystery"
      val request = Get(s"/v1/ebooks?isShownAt=$given")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }

  "language validator" should {
    "handle empty param" in {
      val expected = None
      val request = Get("/v1/ebooks")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.filters.find(_.fieldName == "sourceResource.language.name") shouldEqual expected
      }
    }

    "accept valid param" in {
      val given = "fr"
      val expected = Some("fr")
      val request = Get(s"/v1/ebooks?sourceResource.language.name=$given")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.filters.find(_.fieldName == "sourceResource.language.name")
          .map(_.value) shouldEqual expected
      }
    }

    "reject too-short param" in {
      val given = "d"
      val request = Get(s"/v1/ebooks?sourceResource.language.name=$given")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val request = Get(s"/v1/ebooks?sourceResource.language.name=$given")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }

  "object validator" should {
    "handle empty param" in {
      val expected = None
      val request = Get("/v1/ebooks")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.filters.find(_.fieldName == "object") shouldEqual expected
      }
    }

    "accept valid param" in {
      val given = "http://payload-permanent-address.dp.la"
      val expected = Some("http://payload-permanent-address.dp.la")
      val request = Get(s"/v1/ebooks?object=$given")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.filters.find(_.fieldName == "object").map(_.value) shouldEqual expected
      }
    }

    "reject invalid URL" in {
      val given = "http/payload-permanent-address.dp.la"
      val request = Get(s"/v1/ebooks?object=$given")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }

  "page validator" should {
    "handle empty param" in {
      val expected = 1
      val request = Get("/v1/ebooks")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.page shouldEqual expected
      }
    }

    "accept valid param" in {
      val given = "27"
      val expected = 27
      val request = Get(s"/v1/ebooks?page=$given")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.page shouldEqual expected
      }
    }

    "reject non-int param" in {
      val given = "foo"
      val request = Get(s"/v1/ebooks?page=$given")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }

    "reject out-of-range param" in {
      val given = "0"
      val request = Get(s"/v1/ebooks?page=$given")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }

  "page size validator" should {
    "handle empty param" in {
      val expected = 10
      val request = Get("/v1/ebooks")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.pageSize shouldEqual expected
      }
    }

    "accept valid param" in {
      val given = "50"
      val expected = 50
      val request = Get(s"/v1/ebooks?page_size=$given")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.pageSize shouldEqual expected
      }
    }

    "reject non-int param" in {
      val given = "foo"
      val request = Get(s"/v1/ebooks?page_size=$given")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }

    "reject out-of-range param" in {
      val given = "999999"
      val request = Get(s"/v1/ebooks?page_size=$given")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }

  "publisher validator" should {
    "handle empty param" in {
      val expected = None
      val request = Get("/v1/ebooks")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.filters.find(_.fieldName == "sourceResource.publisher") shouldEqual expected
      }
    }

    "accept valid param" in {
      val given = "Penguin"
      val expected = Some("Penguin")
      val request = Get(s"/v1/ebooks?sourceResource.publisher=$given")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.filters.find(_.fieldName == "sourceResource.publisher")
          .map(_.value) shouldEqual expected
      }
    }

    "reject too-short param" in {
      val given = "d"
      val request = Get(s"/v1/ebooks?sourceResource.publisher=$given")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val request = Get(s"/v1/ebooks?sourceResource.publisher=$given")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }

  "q validator" should {
    "handle empty param" in {
      val expected = None
      val request = Get("/v1/ebooks")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.q shouldEqual expected
      }
    }

    "accept valid param" in {
      val given = "dogs"
      val expected = Some("dogs")
      val request = Get(s"/v1/ebooks?q=$given")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.q shouldEqual expected
      }
    }

    "reject too-short param" in {
      val given = "d"
      val request = Get(s"/v1/ebooks?q=$given")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val request = Get(s"/v1/ebooks?q=$given")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }

  "subject validator" should {
    "handle empty param" in {
      val expected = None
      val request = Get("/v1/ebooks")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.filters
          .find(_.fieldName == "sourceResource.subject.name") shouldEqual expected
      }
    }

    "accept valid param" in {
      val given = "dogs"
      val expected = Some("dogs")
      val request = Get(s"/v1/ebooks?sourceResource.subject.name=$given")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.filters.find(_.fieldName == "sourceResource.subject.name")
          .map(_.value) shouldEqual expected
      }
    }

    "reject too-short param" in {
      val given = "d"
      val request = Get(s"/v1/ebooks?sourceResource.subject.name=$given")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val request = Get(s"/v1/ebooks?sourceResource.subject.name=$given")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }

  "subtitle validator" should {
    "handle empty param" in {
      val expected = None
      val request = Get("/v1/ebooks")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.filters.find(_.fieldName == "sourceResource.subtitle") shouldEqual expected
      }
    }

    "accept valid param" in {
      val given = "A play in three acts".replace(" ", "%20")
      val expected = Some("A play in three acts")
      val request = Get(s"/v1/ebooks?sourceResource.subtitle=$given")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.filters.find(_.fieldName == "sourceResource.subtitle")
          .map(_.value) shouldEqual expected
      }
    }

    "reject too-short param" in {
      val given = "d"
      val request = Get(s"/v1/ebooks?sourceResource.subtitle=$given")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val request = Get(s"/v1/ebooks?sourceResource.subtitle=$given")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }

  "title validator" should {
    "handle empty param" in {
      val expected = None
      val request = Get("/v1/ebooks")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.filters.find(_.fieldName == "sourceResource.title") shouldEqual expected
      }
    }

    "accept valid param" in {
      val given = "The Scarlet Letter".replace(" ", "%20")
      val expected = Some("The Scarlet Letter")
      val request = Get(s"/v1/ebooks?sourceResource.title=$given")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.filters.find(_.fieldName == "sourceResource.title")
          .map(_.value) shouldEqual expected
      }
    }

    "reject too-short param" in {
      val given = "d"
      val request = Get(s"/v1/ebooks?sourceResource.title=$given")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val request = Get(s"/v1/ebooks?sourceResource.title=$given")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }
}
