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

import scala.util.{Failure, Random, Success}

class ParamValidationTest extends AnyWordSpec with Matchers with ScalatestRouteTest with Mocks {

  val minRawParams: RawParams = RawParams(
    creator = None,
    dataProvider = None,
    date = None,
    description = None,
    exactFieldMatch = None,
    facets = None,
    facetSize = None,
    format = None,
    isShownAt = None,
    language = None,
    `object` = None,
    page = None,
    pageSize = None,
    publisher = None,
    q = None,
    subject = None,
    subtitle = None,
    title = None
  )

  def expectSuccess(raw: RawParams): SearchParams =
    ParamValidator.getSearchParams(raw) match {
      case Success(p) => p
      case Failure(_) => throw new RuntimeException("unexpected validation error")
    }

  def getFilterValue(raw: RawParams, fieldName: String): Option[String] =
    expectSuccess(raw).filters.find(_.fieldName == fieldName).map(_.value)




  lazy val testKit: ActorTestKit = ActorTestKit()
  implicit def typedSystem: ActorSystem[Nothing] = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem = testKit.system.classicSystem

  val elasticSearchClient: MockElasticSearchClient = getMockElasticSearchClient
  lazy val routes: Route = new Routes(elasticSearchClient).applicationRoutes

  "ebook ID validator" should {
    "accept valid ID" in {
      val given = "R0VfVX4BfY91SSpFGqxt"
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

    "handle too-long param" in {
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

    "handle invalid URL" in {
      val given = Some("standardebooks")
      val raw = minRawParams.copy(dataProvider = given)
      val validated = ParamValidator.getSearchParams(raw)
      assert(validated.isFailure)
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

    "handle too-short param" in {
      val given = Some("1")
      val raw = minRawParams.copy(date=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert (validated.isFailure)
    }

    "handle too-long param" in {
      val given = Some(
        """
          |"Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et
          | dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip
          | ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu
          | fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt
          | mollit anim id est laborum.
          | """.stripMargin)
      val raw = minRawParams.copy(date=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert (validated.isFailure)
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

    "handle too-short param" in {
      val given = Some("d")
      val raw = minRawParams.copy(description=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert (validated.isFailure)
    }

    "handle too-long param" in {
      val given = Some(
        """
          |"Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et
          | dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip
          | ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu
          | fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt
          | mollit anim id est laborum.
          | """.stripMargin)
      val raw = minRawParams.copy(description=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert (validated.isFailure)
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

    "handle non-boolean param" in {
      val given = Some("foo")
      val raw = minRawParams.copy(exactFieldMatch=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert(validated.isFailure)
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

    "handle unfacetable field" in {
      val given = Some("sourceResource.description")
      val raw = minRawParams.copy(facets=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert(validated.isFailure)
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

    "handle non-int param" in {
      val given = Some("foo")
      val raw = minRawParams.copy(page=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert(validated.isFailure)
    }

    "handle out-of-range param" in {
      val given = Some("9999")
      val raw = minRawParams.copy(facetSize=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert(validated.isFailure)
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

    "handle too-short param" in {
      val given = Some("d")
      val raw = minRawParams.copy(format=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert (validated.isFailure)
    }

    "handle too-long param" in {
      val given = Some(
        """
          |"Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et
          | dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip
          | ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu
          | fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt
          | mollit anim id est laborum.
          | """.stripMargin)
      val raw = minRawParams.copy(format=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert (validated.isFailure)
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

    "handle invalid URL" in {
      val given = Some("the-charing-cross-mystery")
      val raw = minRawParams.copy(isShownAt = given)
      val validated = ParamValidator.getSearchParams(raw)
      assert(validated.isFailure)
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

    "handle too-short param" in {
      val given = Some("d")
      val raw = minRawParams.copy(language=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert (validated.isFailure)
    }

    "handle too-long param" in {
      val given = Some(
        """
          |"Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et
          | dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip
          | ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu
          | fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt
          | mollit anim id est laborum.
          | """.stripMargin)
      val raw = minRawParams.copy(language=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert (validated.isFailure)
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
      val given = "\"http://payload-permanent-address.dp.la\""
      val expected = Some("\"http://payload-permanent-address.dp.la\"")
      val request = Get(s"/v1/ebooks?object=$given")

      request ~> Route.seal(routes) ~> check {
        elasticSearchClient.getLastParams.filters.find(_.fieldName == "object").map(_.value) shouldEqual expected
      }
    }

    "handle invalid URL" in {
      val given = Some("http/payload-permanent-address.dp.la")
      val raw = minRawParams.copy(`object` = given)
      val validated = ParamValidator.getSearchParams(raw)
      assert(validated.isFailure)
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

    "handle non-int param" in {
      val given = Some("foo")
      val raw = minRawParams.copy(page=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert(validated.isFailure)
    }

    "handle out-of-range param" in {
      val given = Some("0")
      val raw = minRawParams.copy(page=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert(validated.isFailure)
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

    "handle non-int param" in {
      val given = Some("foo")
      val raw = minRawParams.copy(pageSize=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert(validated.isFailure)
    }

    "handle out-of-range param" in {
      val given = Some("999999")
      val raw = minRawParams.copy(pageSize=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert(validated.isFailure)
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

    "handle too-short param" in {
      val given = Some("d")
      val raw = minRawParams.copy(publisher=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert (validated.isFailure)
    }

    "handle too-long param" in {
      val given = Some(
        """
          |"Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et
          | dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip
          | ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu
          | fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt
          | mollit anim id est laborum.
          | """.stripMargin)
      val raw = minRawParams.copy(publisher=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert (validated.isFailure)
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

    "handle too-short param" in {
      val given = Some("d")
      val raw = minRawParams.copy(q=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert (validated.isFailure)
    }

    "handle too-long param" in {
      val given = Some(
        """
          |"Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et
          | dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip
          | ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu
          | fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt
          | mollit anim id est laborum.
          | """.stripMargin)
      val raw = minRawParams.copy(q=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert (validated.isFailure)
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

    "handle too-short param" in {
      val given = Some("d")
      val raw = minRawParams.copy(subject=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert (validated.isFailure)
    }

    "handle too-long param" in {
      val given = Some(
        """
          |"Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et
          | dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip
          | ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu
          | fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt
          | mollit anim id est laborum.
          | """.stripMargin)
      val raw = minRawParams.copy(subject=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert (validated.isFailure)
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

    "handle too-short param" in {
      val given = Some("d")
      val raw = minRawParams.copy(subtitle=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert (validated.isFailure)
    }

    "handle too-long param" in {
      val given = Some(
        """
          |"Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et
          | dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip
          | ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu
          | fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt
          | mollit anim id est laborum.
          | """.stripMargin)
      val raw = minRawParams.copy(subtitle=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert (validated.isFailure)
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

    "handle too-short param" in {
      val given = Some("d")
      val raw = minRawParams.copy(title=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert (validated.isFailure)
    }

    "handle too-long param" in {
      val given = Some(
        """
          |"Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et
          | dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip
          | ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu
          | fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt
          | mollit anim id est laborum.
          | """.stripMargin)
      val raw = minRawParams.copy(title=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert (validated.isFailure)
    }
  }
}
