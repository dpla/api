package dpla.ebookapi.v1.ebooks.unit

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import dpla.ebookapi.v1.ebooks.{EbookParamValidator, InvalidApiKey, ValidFetchParams, ValidSearchParams}
import dpla.ebookapi.v1.{InvalidParams, ValidationResponse}
import dpla.ebookapi.v1.ebooks.EbookParamValidator.{ValidateFetchParams, ValidateSearchParams, EbookValidationCommand}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.Random


class EbookParamValidatorTest extends AnyWordSpec with Matchers
  with BeforeAndAfterAll {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit()

  val baseParams = Map("api_key" -> "08e3918eeb8bf4469924f062072459a8")

  val paramValidator: ActorRef[EbookValidationCommand] =
    testKit.spawn(EbookParamValidator())
  val probe: TestProbe[ValidationResponse] =
    testKit.createTestProbe[ValidationResponse]

  "search param validator" should {
    "reject unrecognized params" in {
      val params = baseParams ++ Map("foo" -> "bar")
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "fetch param validator" should {
    "reject unrecognized params" in {
      val id = "R0VfVX4BfY91SSpFGqxt"
      val params = baseParams ++ Map("foo" -> "bar")
      paramValidator ! ValidateFetchParams(id, params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "ebook ID validator" should {
    "accept valid ID" in {
      val id = "ufwPJ34Bj-MaVWqX9KZL"
      paramValidator ! ValidateFetchParams(id, baseParams, probe.ref)
      val msg = probe.expectMessageType[ValidFetchParams]
      assert(msg.fetchParams.id == id)
    }

    "reject ID with special characters" in {
      val id = "<foo>"
      paramValidator ! ValidateFetchParams(id, baseParams, probe.ref)
      probe.expectMessageType[InvalidParams]
    }

    "reject too-long ID" in {
      val id = "asdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdf"
      paramValidator ! ValidateFetchParams(id, baseParams, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "creator validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(baseParams, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "sourceResource.creator")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "Jules Verne"
      val expected = Some("Jules Verne")
      val params = baseParams ++ Map("sourceResource.creator" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "sourceResource.creator").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = baseParams ++ Map("sourceResource.creator" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = baseParams ++ Map("sourceResource.creator" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "provider id validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(baseParams, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "provider.@id")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "https://standardebooks.org"
      val expected = Some("https://standardebooks.org")
      val params = baseParams ++ Map("provider.@id" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "provider.@id").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject invalid URL" in {
      val given = "standardebooks"
      val params = baseParams ++ Map("provider.@id" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "date validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(baseParams, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "sourceResource.date.displayDate")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "2002"
      val expected = Some("2002")
      val params = baseParams ++ Map("sourceResource.date.displayDate" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "sourceResource.date.displayDate").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "1"
      val params = baseParams ++ Map("sourceResource.date.displayDate" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = baseParams ++ Map("sourceResource.date.displayDate" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "description validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(baseParams, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "sourceResource.description")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "dogs"
      val expected = Some("dogs")
      val params = baseParams ++ Map("sourceResource.description" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "sourceResource.description").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = baseParams ++ Map("sourceResource.description" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = baseParams ++ Map("sourceResource.description" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "exact_field_match validator" should {
    "handle empty param" in {
      val expected = false
      paramValidator ! ValidateSearchParams(baseParams, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.exactFieldMatch shouldEqual expected
    }

    "accept valid param" in {
      val given = "true"
      val expected = true
      val params = baseParams ++ Map("exact_field_match" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.exactFieldMatch shouldEqual expected
    }

    "reject non-boolean param" in {
      val given = "yes"
      val params = baseParams ++ Map("exact_field_match" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "facet validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(baseParams, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.facets shouldEqual expected
    }

    "accept valid param" in {
      val given = "provider.@id,sourceResource.creator"
      val expected = Some(Seq("provider.@id", "sourceResource.creator"))
      val params = baseParams ++ Map("facets" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.facets shouldEqual expected
    }

    "reject unfacetable field" in {
      val given = "sourceResource.description"
      val params = baseParams ++ Map("facets" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "facet size validator" should {
    "handle empty param" in {
      val expected = 50
      paramValidator ! ValidateSearchParams(baseParams, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.facetSize shouldEqual expected
    }

    "accept valid param" in {
      val given = "30"
      val expected = 30
      val params = baseParams ++ Map("facet_size" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.facetSize shouldEqual expected
    }

    "reject non-int param" in {
      val given = "foo"
      val params = baseParams ++ Map("facet_size" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }

    "reject out-of-range param" in {
      val given = "9999"
      val params = baseParams ++ Map("facet_size" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "fields validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(baseParams, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.fields shouldEqual expected
    }

    "accept valid param" in {
      val given = "provider.@id,sourceResource.creator"
      val expected = Some(Seq("provider.@id", "sourceResource.creator"))
      val params = baseParams ++ Map("fields" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.fields shouldEqual expected
    }

    "reject unrecognized field" in {
      val given = "foo"
      val params = baseParams ++ Map("fields" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "format validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(baseParams, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "sourceResource.format")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "article"
      val expected = Some("article")
      val params = baseParams ++ Map("sourceResource.format" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "sourceResource.format").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = baseParams ++ Map("sourceResource.format" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = baseParams ++ Map("sourceResource.format" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "isShownAt validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(baseParams, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "isShownAt")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "\"https://standardebooks.org/ebooks/j-s-fletcher/the-charing-cross-mystery\""
      val expected = Some("\"https://standardebooks.org/ebooks/j-s-fletcher/the-charing-cross-mystery\"")
      val params = baseParams ++ Map("isShownAt" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "isShownAt").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject invalid URL" in {
      val given = "the-charing-cross-mystery"
      val params = baseParams ++ Map("isShownAt" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "language validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(baseParams, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "sourceResource.language.name")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "fr"
      val expected = Some("fr")
      val params = baseParams ++ Map("sourceResource.language.name" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "sourceResource.language.name").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = baseParams ++ Map("sourceResource.language.name" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = baseParams ++ Map("sourceResource.language.name" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "object validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(baseParams, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "object")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "http://payload-permanent-address.dp.la"
      val expected = Some("http://payload-permanent-address.dp.la")
      val params = baseParams ++ Map("object" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "object").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject invalid URL" in {
      val given = "http/payload-permanent-address.dp.la"
      val params = baseParams ++ Map("object" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "op validator" should {
    "handle empty param" in {
      val expected = "AND"
      paramValidator ! ValidateSearchParams(baseParams, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.op shouldEqual expected
    }

    "accept valid param" in {
      val given = "OR"
      val expected = "OR"
      val params = baseParams ++ Map("op" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.op shouldEqual expected
    }

    "reject invalid param" in {
      val given = "or"
      val params = baseParams ++ Map("op" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "page validator" should {
    "handle empty param" in {
      val expected = 1
      paramValidator ! ValidateSearchParams(baseParams, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.page shouldEqual expected
    }

    "accept valid param" in {
      val given = "27"
      val expected = 27
      val params = baseParams ++ Map("page" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.page shouldEqual expected
    }

    "reject non-int param" in {
      val given = "foo"
      val params = baseParams ++ Map("page" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }

    "reject out-of-range param" in {
      val given = "0"
      val params = baseParams ++ Map("page" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "page size validator" should {
    "handle empty param" in {
      val expected = 10
      paramValidator ! ValidateSearchParams(baseParams, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.pageSize shouldEqual expected
    }

    "accept valid param" in {
      val given = "50"
      val expected = 50
      val params = baseParams ++ Map("page_size" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.pageSize shouldEqual expected
    }

    "reject non-int param" in {
      val given = "foo"
      val params = baseParams ++ Map("page_size" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }

    "reject out-of-range param" in {
      val given = "999999"
      val params = baseParams ++ Map("page_size" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "publisher validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(baseParams, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "sourceResource.publisher")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "Penguin"
      val expected = Some("Penguin")
      val params = baseParams ++ Map("sourceResource.publisher" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "sourceResource.publisher").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = baseParams ++ Map("sourceResource.publisher" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = baseParams ++ Map("sourceResource.publisher" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "q validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(baseParams, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.q shouldEqual expected
    }

    "accept valid param" in {
      val given = "dogs"
      val expected = Some("dogs")
      val params = baseParams ++ Map("q" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.q shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = baseParams ++ Map("q" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = baseParams ++ Map("q" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "sortBy validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(baseParams, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.sortBy shouldEqual expected
    }

    "accept valid param" in {
      val given = "sourceResource.title"
      val expected = Some("sourceResource.title")
      val params = baseParams ++ Map("sort_by" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.sortBy shouldEqual expected
    }

    "reject invalid param" in {
      val given = "sourceResource.description"
      val params = baseParams ++ Map("sort_by" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "sortOrder validator" should {
    "handle empty param" in {
      val expected = "asc"
      paramValidator ! ValidateSearchParams(baseParams, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.sortOrder shouldEqual expected
    }

    "accept valid param" in {
      val given = "desc"
      val expected = "desc"
      val params = baseParams ++ Map("sort_order" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.sortOrder shouldEqual expected
    }

    "reject invalid param" in {
      val given = "descending"
      val params = baseParams ++ Map("sort_order" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "subject validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(baseParams, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "sourceResource.subject.name")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "dogs"
      val expected = Some("dogs")
      val params = baseParams ++ Map("sourceResource.subject.name" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "sourceResource.subject.name").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = baseParams ++ Map("sourceResource.subject.name" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = baseParams ++ Map("sourceResource.subject.name" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "subtitle validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(baseParams, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue =msg.searchParams.filters
        .find(_.fieldName == "sourceResource.subtitle")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "A play in three acts"
      val expected = Some("A play in three acts")
      val params = baseParams ++ Map("sourceResource.subtitle" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "sourceResource.subtitle").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = baseParams ++ Map("sourceResource.subtitle" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = baseParams ++ Map("sourceResource.subtitle" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "title validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(baseParams, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "sourceResource.title")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "The Scarlet Letter"
      val expected = Some("The Scarlet Letter")
      val params = baseParams ++ Map("sourceResource.title" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "sourceResource.title").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = baseParams ++ Map("sourceResource.title" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = baseParams ++ Map("sourceResource.title" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }
}
