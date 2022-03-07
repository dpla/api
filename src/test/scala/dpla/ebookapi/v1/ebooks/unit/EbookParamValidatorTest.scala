package dpla.ebookapi.v1.ebooks.unit

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import dpla.ebookapi.v1.ebooks.{EbookParamValidator, EbookParamValidatorResponse, InvalidEbookParams, ValidFetchParams, ValidSearchParams}
import dpla.ebookapi.v1.ebooks.EbookParamValidator.{EbookParamValidatorCommand, ValidateFetchParams, ValidateSearchParams}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.Random


class EbookParamValidatorTest extends AnyWordSpec with Matchers
  with BeforeAndAfterAll {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit()

  val paramValidator: ActorRef[EbookParamValidatorCommand] =
    testKit.spawn(EbookParamValidator())
  val probe: TestProbe[EbookParamValidatorResponse] =
    testKit.createTestProbe[EbookParamValidatorResponse]

  "search param validator" should {
    "reject unrecognized params" in {
      val params = Map("foo" -> "bar")
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }
  }

  "fetch param validator" should {
    "reject unrecognized params" in {
      val id = "R0VfVX4BfY91SSpFGqxt"
      val params = Map("foo" -> "bar")
      paramValidator ! ValidateFetchParams(id, params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }
  }

  "ebook ID validator" should {
    "accept valid ID" in {
      val id = "ufwPJ34Bj-MaVWqX9KZL"
      paramValidator ! ValidateFetchParams(id, Map(), probe.ref)
      val msg = probe.expectMessageType[ValidFetchParams]
      assert(msg.fetchParams.id == id)
    }

    "reject ID with special characters" in {
      val id = "<foo>"
      paramValidator ! ValidateFetchParams(id, Map(), probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }

    "reject too-long ID" in {
      val id = "asdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdf"
      paramValidator ! ValidateFetchParams(id, Map(), probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }
  }

  "creator validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(Map(), probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "sourceResource.creator")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "Jules Verne"
      val expected = Some("Jules Verne")
      val params = Map("sourceResource.creator" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "sourceResource.creator").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = Map("sourceResource.creator" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.creator" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }
  }

  "provider id validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(Map(), probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "provider.@id")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "https://standardebooks.org"
      val expected = Some("https://standardebooks.org")
      val params = Map("provider.@id" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "provider.@id").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject invalid URL" in {
      val given = "standardebooks"
      val params = Map("provider.@id" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }
  }

  "date validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(Map(), probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "sourceResource.date.displayDate")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "2002"
      val expected = Some("2002")
      val params = Map("sourceResource.date.displayDate" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "sourceResource.date.displayDate").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "1"
      val params = Map("sourceResource.date.displayDate" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.date.displayDate" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }
  }

  "description validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(Map(), probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "sourceResource.description")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "dogs"
      val expected = Some("dogs")
      val params = Map("sourceResource.description" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "sourceResource.description").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = Map("sourceResource.description" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.description" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }
  }

  "exact_field_match validator" should {
    "handle empty param" in {
      val expected = false
      paramValidator ! ValidateSearchParams(Map(), probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.exactFieldMatch shouldEqual expected
    }

    "accept valid param" in {
      val given = "true"
      val expected = true
      val params = Map("exact_field_match" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.exactFieldMatch shouldEqual expected
    }

    "reject non-boolean param" in {
      val given = "yes"
      val params = Map("exact_field_match" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }
  }

  "facet validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(Map(), probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.facets shouldEqual expected
    }

    "accept valid param" in {
      val given = "provider.@id,sourceResource.creator"
      val expected = Some(Seq("provider.@id", "sourceResource.creator"))
      val params = Map("facets" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.facets shouldEqual expected
    }

    "reject unfacetable field" in {
      val given = "sourceResource.description"
      val params = Map("facets" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }
  }

  "facet size validator" should {
    "handle empty param" in {
      val expected = 50
      paramValidator ! ValidateSearchParams(Map(), probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.facetSize shouldEqual expected
    }

    "accept valid param" in {
      val given = "30"
      val expected = 30
      val params = Map("facet_size" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.facetSize shouldEqual expected
    }

    "reject non-int param" in {
      val given = "foo"
      val params = Map("facet_size" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }

    "reject out-of-range param" in {
      val given = "9999"
      val params = Map("facet_size" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }
  }

  "fields validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(Map(), probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.fields shouldEqual expected
    }

    "accept valid param" in {
      val given = "provider.@id,sourceResource.creator"
      val expected = Some(Seq("provider.@id", "sourceResource.creator"))
      val params = Map("fields" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.fields shouldEqual expected
    }

    "reject unrecognized field" in {
      val given = "foo"
      val params = Map("fields" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }
  }

  "format validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(Map(), probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "sourceResource.format")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "article"
      val expected = Some("article")
      val params = Map("sourceResource.format" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "sourceResource.format").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = Map("sourceResource.format" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.format" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }
  }

  "isShownAt validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(Map(), probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "isShownAt")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "\"https://standardebooks.org/ebooks/j-s-fletcher/the-charing-cross-mystery\""
      val expected = Some("\"https://standardebooks.org/ebooks/j-s-fletcher/the-charing-cross-mystery\"")
      val params = Map("isShownAt" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "isShownAt").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject invalid URL" in {
      val given = "the-charing-cross-mystery"
      val params = Map("isShownAt" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }
  }

  "language validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(Map(), probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "sourceResource.language.name")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "fr"
      val expected = Some("fr")
      val params = Map("sourceResource.language.name" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "sourceResource.language.name").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = Map("sourceResource.language.name" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.language.name" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }
  }

  "object validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(Map(), probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "object")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "http://payload-permanent-address.dp.la"
      val expected = Some("http://payload-permanent-address.dp.la")
      val params = Map("object" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "object").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject invalid URL" in {
      val given = "http/payload-permanent-address.dp.la"
      val params = Map("object" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }
  }

  "op validator" should {
    "handle empty param" in {
      val expected = "AND"
      paramValidator ! ValidateSearchParams(Map(), probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.op shouldEqual expected
    }

    "accept valid param" in {
      val given = "OR"
      val expected = "OR"
      val params = Map("op" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.op shouldEqual expected
    }

    "reject invalid param" in {
      val given = "or"
      val params = Map("op" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }
  }

  "page validator" should {
    "handle empty param" in {
      val expected = 1
      paramValidator ! ValidateSearchParams(Map(), probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.page shouldEqual expected
    }

    "accept valid param" in {
      val given = "27"
      val expected = 27
      val params = Map("page" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.page shouldEqual expected
    }

    "reject non-int param" in {
      val given = "foo"
      val params = Map("page" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }

    "reject out-of-range param" in {
      val given = "0"
      val params = Map("page" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }
  }

  "page size validator" should {
    "handle empty param" in {
      val expected = 10
      paramValidator ! ValidateSearchParams(Map(), probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.pageSize shouldEqual expected
    }

    "accept valid param" in {
      val given = "50"
      val expected = 50
      val params = Map("page_size" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.pageSize shouldEqual expected
    }

    "reject non-int param" in {
      val given = "foo"
      val params = Map("page_size" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }

    "reject out-of-range param" in {
      val given = "999999"
      val params = Map("page_size" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }
  }

  "publisher validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(Map(), probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "sourceResource.publisher")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "Penguin"
      val expected = Some("Penguin")
      val params = Map("sourceResource.publisher" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "sourceResource.publisher").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = Map("sourceResource.publisher" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.publisher" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }
  }

  "q validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(Map(), probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.q shouldEqual expected
    }

    "accept valid param" in {
      val given = "dogs"
      val expected = Some("dogs")
      val params = Map("q" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.q shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = Map("q" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("q" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }
  }

  "sortBy validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(Map(), probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.sortBy shouldEqual expected
    }

    "accept valid param" in {
      val given = "sourceResource.title"
      val expected = Some("sourceResource.title")
      val params = Map("sort_by" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.sortBy shouldEqual expected
    }

    "reject invalid param" in {
      val given = "sourceResource.description"
      val params = Map("sort_by" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }
  }

  "sortOrder validator" should {
    "handle empty param" in {
      val expected = "asc"
      paramValidator ! ValidateSearchParams(Map(), probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.sortOrder shouldEqual expected
    }

    "accept valid param" in {
      val given = "desc"
      val expected = "desc"
      val params = Map("sort_order" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.sortOrder shouldEqual expected
    }

    "reject invalid param" in {
      val given = "descending"
      val params = Map("sort_order" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }
  }

  "subject validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(Map(), probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "sourceResource.subject.name")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "dogs"
      val expected = Some("dogs")
      val params = Map("sourceResource.subject.name" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "sourceResource.subject.name").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params =  Map("sourceResource.subject.name" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.subject.name" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }
  }

  "subtitle validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(Map(), probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue =msg.searchParams.filters
        .find(_.fieldName == "sourceResource.subtitle")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "A play in three acts"
      val expected = Some("A play in three acts")
      val params = Map("sourceResource.subtitle" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "sourceResource.subtitle").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = Map("sourceResource.subtitle" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.subtitle" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }
  }

  "title validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(Map(), probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "sourceResource.title")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "The Scarlet Letter"
      val expected = Some("The Scarlet Letter")
      val params = Map("sourceResource.title" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.searchParams.filters
        .find(_.fieldName == "sourceResource.title").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = Map("sourceResource.title" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.title" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidEbookParams]
    }
  }
}
