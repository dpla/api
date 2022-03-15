package dpla.ebookapi.v1.search

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import dpla.ebookapi.v1.search.SearchProtocol.{IntermediateSearchResult, InvalidSearchParams, RawFetchParams, RawSearchParams, SearchResponse, ValidFetchIds, ValidSearchParams}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.Random


class EbookParamValidatorTest extends AnyWordSpec with Matchers
  with BeforeAndAfterAll {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit()

  val replyProbe: TestProbe[SearchResponse] =
    testKit.createTestProbe[SearchResponse]

  val interProbe: TestProbe[IntermediateSearchResult] =
    testKit.createTestProbe[IntermediateSearchResult]

  val paramValidator: ActorRef[IntermediateSearchResult] =
    testKit.spawn(EbookParamValidator(interProbe.ref))

  "search param validator" should {
    "reject unrecognized params" in {
      val params = Map("foo" -> "bar")
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "fetch param validator" should {
    "reject unrecognized params" in {
      val id = "R0VfVX4BfY91SSpFGqxt"
      val params = Map("foo" -> "bar")
      paramValidator ! RawFetchParams(id, params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "ebook ID validator" should {
    "accept valid ID" in {
      val id = "ufwPJ34Bj-MaVWqX9KZL"
      paramValidator ! RawFetchParams(id, Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidFetchIds]
      msg.ids should contain only id
    }

    "reject ID with special characters" in {
      val id = "<foo>"
      paramValidator ! RawFetchParams(id, Map(), replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "reject too-long ID" in {
      val id = "asdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdf"
      paramValidator ! RawFetchParams(id, Map(), replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "creator validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.filters
        .find(_.fieldName == "sourceResource.creator")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "Jules Verne"
      val expected = Some("Jules Verne")
      val params = Map("sourceResource.creator" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.filters
        .find(_.fieldName == "sourceResource.creator").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = Map("sourceResource.creator" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.creator" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "provider id validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.filters
        .find(_.fieldName == "provider.@id")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "https://standardebooks.org"
      val expected = Some("https://standardebooks.org")
      val params = Map("provider.@id" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.filters
        .find(_.fieldName == "provider.@id").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject invalid URL" in {
      val given = "standardebooks"
      val params = Map("provider.@id" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "date validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.filters
        .find(_.fieldName == "sourceResource.date.displayDate")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "2002"
      val expected = Some("2002")
      val params = Map("sourceResource.date.displayDate" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.filters
        .find(_.fieldName == "sourceResource.date.displayDate").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "1"
      val params = Map("sourceResource.date.displayDate" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.date.displayDate" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "description validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.filters
        .find(_.fieldName == "sourceResource.description")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "dogs"
      val expected = Some("dogs")
      val params = Map("sourceResource.description" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.filters
        .find(_.fieldName == "sourceResource.description").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = Map("sourceResource.description" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.description" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "exact_field_match validator" should {
    "handle empty param" in {
      val expected = false
      paramValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.exactFieldMatch shouldEqual expected
    }

    "accept valid param" in {
      val given = "true"
      val expected = true
      val params = Map("exact_field_match" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.exactFieldMatch shouldEqual expected
    }

    "reject non-boolean param" in {
      val given = "yes"
      val params = Map("exact_field_match" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "facet validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.facets shouldEqual expected
    }

    "accept valid param" in {
      val given = "provider.@id,sourceResource.creator"
      val expected = Some(Seq("provider.@id", "sourceResource.creator"))
      val params = Map("facets" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.facets shouldEqual expected
    }

    "reject unfacetable field" in {
      val given = "sourceResource.description"
      val params = Map("facets" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "facet size validator" should {
    "handle empty param" in {
      val expected = 50
      paramValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.facetSize shouldEqual expected
    }

    "accept valid param" in {
      val given = "30"
      val expected = 30
      val params = Map("facet_size" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.facetSize shouldEqual expected
    }

    "reject non-int param" in {
      val given = "foo"
      val params = Map("facet_size" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "reject out-of-range param" in {
      val given = "9999"
      val params = Map("facet_size" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "fields validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.fields shouldEqual expected
    }

    "accept valid param" in {
      val given = "provider.@id,sourceResource.creator"
      val expected = Some(Seq("provider.@id", "sourceResource.creator"))
      val params = Map("fields" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.fields shouldEqual expected
    }

    "reject unrecognized field" in {
      val given = "foo"
      val params = Map("fields" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "format validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.filters
        .find(_.fieldName == "sourceResource.format")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "article"
      val expected = Some("article")
      val params = Map("sourceResource.format" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.filters
        .find(_.fieldName == "sourceResource.format").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = Map("sourceResource.format" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.format" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "isShownAt validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.filters
        .find(_.fieldName == "isShownAt")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "\"https://standardebooks.org/ebooks/j-s-fletcher/the-charing-cross-mystery\""
      val expected = Some("\"https://standardebooks.org/ebooks/j-s-fletcher/the-charing-cross-mystery\"")
      val params = Map("isShownAt" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.filters
        .find(_.fieldName == "isShownAt").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject invalid URL" in {
      val given = "the-charing-cross-mystery"
      val params = Map("isShownAt" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "language validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.filters
        .find(_.fieldName == "sourceResource.language.name")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "fr"
      val expected = Some("fr")
      val params = Map("sourceResource.language.name" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.filters
        .find(_.fieldName == "sourceResource.language.name").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = Map("sourceResource.language.name" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.language.name" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "object validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.filters
        .find(_.fieldName == "object")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "http://payload-permanent-address.dp.la"
      val expected = Some("http://payload-permanent-address.dp.la")
      val params = Map("object" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.filters
        .find(_.fieldName == "object").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject invalid URL" in {
      val given = "http/payload-permanent-address.dp.la"
      val params = Map("object" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "op validator" should {
    "handle empty param" in {
      val expected = "AND"
      paramValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.op shouldEqual expected
    }

    "accept valid param" in {
      val given = "OR"
      val expected = "OR"
      val params = Map("op" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.op shouldEqual expected
    }

    "reject invalid param" in {
      val given = "or"
      val params = Map("op" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "page validator" should {
    "handle empty param" in {
      val expected = 1
      paramValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.page shouldEqual expected
    }

    "accept valid param" in {
      val given = "27"
      val expected = 27
      val params = Map("page" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.page shouldEqual expected
    }

    "reject non-int param" in {
      val given = "foo"
      val params = Map("page" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "reject out-of-range param" in {
      val given = "0"
      val params = Map("page" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "page size validator" should {
    "handle empty param" in {
      val expected = 10
      paramValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.pageSize shouldEqual expected
    }

    "accept valid param" in {
      val given = "50"
      val expected = 50
      val params = Map("page_size" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.pageSize shouldEqual expected
    }

    "reject non-int param" in {
      val given = "foo"
      val params = Map("page_size" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "reject out-of-range param" in {
      val given = "999999"
      val params = Map("page_size" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "publisher validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.filters
        .find(_.fieldName == "sourceResource.publisher")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "Penguin"
      val expected = Some("Penguin")
      val params = Map("sourceResource.publisher" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.filters
        .find(_.fieldName == "sourceResource.publisher").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = Map("sourceResource.publisher" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.publisher" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "q validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.q shouldEqual expected
    }

    "accept valid param" in {
      val given = "dogs"
      val expected = Some("dogs")
      val params = Map("q" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.q shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = Map("q" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("q" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "sortBy validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.sortBy shouldEqual expected
    }

    "accept valid param" in {
      val given = "sourceResource.title"
      val expected = Some("sourceResource.title")
      val params = Map("sort_by" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.sortBy shouldEqual expected
    }

    "reject invalid param" in {
      val given = "sourceResource.description"
      val params = Map("sort_by" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "sortOrder validator" should {
    "handle empty param" in {
      val expected = "asc"
      paramValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.sortOrder shouldEqual expected
    }

    "accept valid param" in {
      val given = "desc"
      val expected = "desc"
      val params = Map("sort_order" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.sortOrder shouldEqual expected
    }

    "reject invalid param" in {
      val given = "descending"
      val params = Map("sort_order" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "subject validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.filters
        .find(_.fieldName == "sourceResource.subject.name")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "dogs"
      val expected = Some("dogs")
      val params = Map("sourceResource.subject.name" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.filters
        .find(_.fieldName == "sourceResource.subject.name").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params =  Map("sourceResource.subject.name" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.subject.name" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "subtitle validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue =msg.params.filters
        .find(_.fieldName == "sourceResource.subtitle")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "A play in three acts"
      val expected = Some("A play in three acts")
      val params = Map("sourceResource.subtitle" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.filters
        .find(_.fieldName == "sourceResource.subtitle").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = Map("sourceResource.subtitle" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.subtitle" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "title validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.filters
        .find(_.fieldName == "sourceResource.title")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "The Scarlet Letter"
      val expected = Some("The Scarlet Letter")
      val params = Map("sourceResource.title" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.filters
        .find(_.fieldName == "sourceResource.title").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = Map("sourceResource.title" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.title" -> given)
      paramValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }
}
