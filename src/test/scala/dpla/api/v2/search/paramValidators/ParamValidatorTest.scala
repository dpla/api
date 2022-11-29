package dpla.api.v2.search.paramValidators

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import dpla.api.v2.search.SearchProtocol._
import org.scalactic.TimesOnInt.convertIntToRepeater
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.Random


class ParamValidatorTest extends AnyWordSpec with Matchers
  with BeforeAndAfterAll {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit()

  val replyProbe: TestProbe[SearchResponse] =
    testKit.createTestProbe[SearchResponse]

  val interProbe: TestProbe[IntermediateSearchResult] =
    testKit.createTestProbe[IntermediateSearchResult]

  val ebookParamValidator: ActorRef[IntermediateSearchResult] =
    testKit.spawn(EbookParamValidator(interProbe.ref))

  val itemParamValidator: ActorRef[IntermediateSearchResult] =
    testKit.spawn(ItemParamValidator(interProbe.ref))

  "search param validator" should {
    "reject unrecognized params" in {
      val params = Map("foo" -> "bar")
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "fetch param validator" should {
    "reject unrecognized params" in {
      val id = "R0VfVX4BfY91SSpFGqxt"
      val params = Map("foo" -> "bar")
      ebookParamValidator ! RawFetchParams(id, params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "random param validator" should {
    "accept valid filter" in {
      val params = Map("filter" -> "provider.@id:http://dp.la/api/contributor/lc")
      itemParamValidator ! RawRandomParams(params, replyProbe.ref)
      interProbe.expectMessageType[ValidRandomParams]
    }

    "reject unrecognized params" in {
      val params = Map("foo" -> "bar")
      itemParamValidator ! RawRandomParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "ID validator" should {
    "accept valid ID" in {
      val id = "ufwPJ34Bj-MaVWqX9KZL"
      ebookParamValidator ! RawFetchParams(id, Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidFetchParams]
      msg.ids should contain only id
    }

    "accept multiple valid IDs" in {
      val ids = "b70107e4fe29fe4a247ae46e118ce192,17b0da7b05805d78daf8753a6641b3f5"
      val expected = Seq("b70107e4fe29fe4a247ae46e118ce192", "17b0da7b05805d78daf8753a6641b3f5")
      ebookParamValidator ! RawFetchParams(ids, Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidFetchParams]
      msg.ids should contain allElementsOf expected
    }

    "reject ID with special characters" in {
      val id = "<foo>"
      ebookParamValidator ! RawFetchParams(id, Map(), replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "reject too-long ID" in {
      val id = "asdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdf"
      ebookParamValidator ! RawFetchParams(id, Map(), replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "reject too many IDs" in {
      var idSeq = Seq[String]()
      501.times { idSeq = idSeq :+ Random.alphanumeric.take(32).mkString }
      val ids = idSeq.mkString(",")
      ebookParamValidator ! RawFetchParams(ids, Map(), replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "date validator" should {
    "handle empty param" in {
      val expected = None
      itemParamValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.fieldQueries
        .find(_.fieldName == "sourceResource.date.begin")
      fieldValue shouldEqual expected
    }

    "accept date as YYYY" in {
      val given = "1900"
      val expected = Some("1900")
      val params = Map("sourceResource.date.begin" -> given)
      itemParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.fieldQueries
        .find(_.fieldName == "sourceResource.date.begin").map(_.value)
      fieldValue shouldEqual expected
    }

    "accept date as YYYY-MM" in {
      val given = "1900-01"
      val expected = Some("1900-01")
      val params = Map("sourceResource.date.begin" -> given)
      itemParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.fieldQueries
        .find(_.fieldName == "sourceResource.date.begin").map(_.value)
      fieldValue shouldEqual expected
    }

    "accept date as YYYY-MM-DD" in {
      val given = "1900-01-01"
      val expected = Some("1900-01-01")
      val params = Map("sourceResource.date.begin" -> given)
      itemParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.fieldQueries
        .find(_.fieldName == "sourceResource.date.begin").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject invalid param" in {
      val given = "190"
      val params = Map("sourceResource.date.begin" -> given)
      itemParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "creator validator" should {
    "handle empty param" in {
      val expected = None
      ebookParamValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.fieldQueries
        .find(_.fieldName == "sourceResource.creator")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "Jules Verne"
      val expected = Some("Jules Verne")
      val params = Map("sourceResource.creator" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.fieldQueries
        .find(_.fieldName == "sourceResource.creator").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = Map("sourceResource.creator" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.creator" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "provider id validator" should {
    "handle empty param" in {
      val expected = None
      ebookParamValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.fieldQueries
        .find(_.fieldName == "provider.@id")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "https://standardebooks.org"
      val expected = Some("https://standardebooks.org")
      val params = Map("provider.@id" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.fieldQueries
        .find(_.fieldName == "provider.@id").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject invalid URL" in {
      val given = "standardebooks"
      val params = Map("provider.@id" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "description validator" should {
    "handle empty param" in {
      val expected = None
      ebookParamValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.fieldQueries
        .find(_.fieldName == "sourceResource.description")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "dogs"
      val expected = Some("dogs")
      val params = Map("sourceResource.description" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.fieldQueries
        .find(_.fieldName == "sourceResource.description").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = Map("sourceResource.description" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.description" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "exact_field_match validator" should {
    "handle empty param" in {
      val expected = false
      ebookParamValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.exactFieldMatch shouldEqual expected
    }

    "accept valid param" in {
      val given = "true"
      val expected = true
      val params = Map("exact_field_match" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.exactFieldMatch shouldEqual expected
    }

    "reject non-boolean param" in {
      val given = "yes"
      val params = Map("exact_field_match" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "facet validator" should {
    "handle empty param" in {
      val expected = None
      ebookParamValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.facets shouldEqual expected
    }

    "accept valid param" in {
      val given = "provider.@id,sourceResource.subject.name"
      val expected = Some(Seq("provider.@id", "sourceResource.subject.name"))
      val params = Map("facets" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.facets shouldEqual expected
    }

    "reject unfacetable field" in {
      val given = "sourceResource.description"
      val params = Map("facets" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "accept valid coordinates param" in {
      val given = "sourceResource.spatial.coordinates:42:-70"
      val expected = Some(Seq("sourceResource.spatial.coordinates:42:-70"))
      val params = Map("facets" -> given)
      itemParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.facets shouldEqual expected
    }
  }

  "facet size validator" should {
    "handle empty param" in {
      val expected = 50
      ebookParamValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.facetSize shouldEqual expected
    }

    "accept valid param" in {
      val given = "30"
      val expected = 30
      val params = Map("facet_size" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.facetSize shouldEqual expected
    }

    "reject non-int param" in {
      val given = "foo"
      val params = Map("facet_size" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "default to max if param is too large" in {
      val given = "9999"
      val expected = 2000
      val params = Map("facet_size" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.facetSize shouldEqual expected
    }
  }

  "fields validator" should {
    "handle empty param" in {
      val expected = None
      ebookParamValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.fields shouldEqual expected
    }

    "accept valid param" in {
      val given = "provider.@id,sourceResource.creator"
      val expected = Some(Seq("provider.@id", "sourceResource.creator"))
      val params = Map("fields" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.fields shouldEqual expected
    }

    "reject unrecognized field" in {
      val given = "foo"
      val params = Map("fields" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "format validator" should {
    "handle empty param" in {
      val expected = None
      ebookParamValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.fieldQueries
        .find(_.fieldName == "sourceResource.format")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "article"
      val expected = Some("article")
      val params = Map("sourceResource.format" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.fieldQueries
        .find(_.fieldName == "sourceResource.format").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = Map("sourceResource.format" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.format" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "isShownAt validator" should {
    "handle empty param" in {
      val expected = None
      ebookParamValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.fieldQueries
        .find(_.fieldName == "isShownAt")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "\"https://standardebooks.org/ebooks/j-s-fletcher/the-charing-cross-mystery\""
      val expected = Some("\"https://standardebooks.org/ebooks/j-s-fletcher/the-charing-cross-mystery\"")
      val params = Map("isShownAt" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.fieldQueries
        .find(_.fieldName == "isShownAt").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject invalid URL" in {
      val given = "the-charing-cross-mystery"
      val params = Map("isShownAt" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "language validator" should {
    "handle empty param" in {
      val expected = None
      ebookParamValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.fieldQueries
        .find(_.fieldName == "sourceResource.language.name")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "fr"
      val expected = Some("fr")
      val params = Map("sourceResource.language.name" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.fieldQueries
        .find(_.fieldName == "sourceResource.language.name").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = Map("sourceResource.language.name" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.language.name" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "object validator" should {
    "handle empty param" in {
      val expected = None
      ebookParamValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.fieldQueries
        .find(_.fieldName == "object")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "http://payload-permanent-address.dp.la"
      val expected = Some("http://payload-permanent-address.dp.la")
      val params = Map("object" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.fieldQueries
        .find(_.fieldName == "object").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject invalid URL" in {
      val given = "http/payload-permanent-address.dp.la"
      val params = Map("object" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "op validator" should {
    "handle empty param" in {
      val expected = "AND"
      ebookParamValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.op shouldEqual expected
    }

    "accept valid param" in {
      val given = "OR"
      val expected = "OR"
      val params = Map("op" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.op shouldEqual expected
    }

    "reject invalid param" in {
      val given = "or"
      val params = Map("op" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "page validator" should {
    "handle empty param" in {
      val expected = 1
      ebookParamValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.page shouldEqual expected
    }

    "accept valid param" in {
      val given = "27"
      val expected = 27
      val params = Map("page" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.page shouldEqual expected
    }

    "reject non-int param" in {
      val given = "foo"
      val params = Map("page" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "reject out-of-range param" in {
      val given = "0"
      val params = Map("page" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "default to max if param is too large" in {
      val given = "600"
      val expected = 100
      val params = Map("page" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.page shouldEqual expected
    }
  }

  "page size validator" should {
    "handle empty param" in {
      val expected = 10
      ebookParamValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.pageSize shouldEqual expected
    }

    "accept valid param" in {
      val given = "50"
      val expected = 50
      val params = Map("page_size" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.pageSize shouldEqual expected
    }

    "reject non-int param" in {
      val given = "foo"
      val params = Map("page_size" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "default to max if param is too large" in {
      val given = "999999"
      val expected = 500
      val params = Map("page_size" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.pageSize shouldEqual expected
    }
  }

  "publisher validator" should {
    "handle empty param" in {
      val expected = None
      ebookParamValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.fieldQueries
        .find(_.fieldName == "sourceResource.publisher")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "Penguin"
      val expected = Some("Penguin")
      val params = Map("sourceResource.publisher" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.fieldQueries
        .find(_.fieldName == "sourceResource.publisher").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = Map("sourceResource.publisher" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.publisher" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "q validator" should {
    "handle empty param" in {
      val expected = None
      ebookParamValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.q shouldEqual expected
    }

    "accept valid param" in {
      val given = "dogs"
      val expected = Some("dogs")
      val params = Map("q" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.q shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = Map("q" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("q" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "sortBy validator" should {
    "handle empty param" in {
      val expected = None
      ebookParamValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.sortBy shouldEqual expected
    }

    "accept valid param" in {
      val given = "sourceResource.title"
      val expected = Some("sourceResource.title")
      val params = Map("sort_by" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.sortBy shouldEqual expected
    }

    "reject invalid param" in {
      val given = "sourceResource.description"
      val params = Map("sort_by" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "sortOrder validator" should {
    "handle empty param" in {
      val expected = "asc"
      ebookParamValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.sortOrder shouldEqual expected
    }

    "accept valid param" in {
      val given = "desc"
      val expected = "desc"
      val params = Map("sort_order" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.sortOrder shouldEqual expected
    }

    "reject invalid param" in {
      val given = "descending"
      val params = Map("sort_order" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "subject validator" should {
    "handle empty param" in {
      val expected = None
      ebookParamValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.fieldQueries
        .find(_.fieldName == "sourceResource.subject.name")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "dogs"
      val expected = Some("dogs")
      val params = Map("sourceResource.subject.name" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.fieldQueries
        .find(_.fieldName == "sourceResource.subject.name").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params =  Map("sourceResource.subject.name" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.subject.name" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "title validator" should {
    "handle empty param" in {
      val expected = None
      ebookParamValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.fieldQueries
        .find(_.fieldName == "sourceResource.title")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "The Scarlet Letter"
      val expected = Some("The Scarlet Letter")
      val params = Map("sourceResource.title" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.fieldQueries
        .find(_.fieldName == "sourceResource.title").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = Map("sourceResource.title" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.title" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "spatial validator" should {

    "accept valid combination of sort_by and sort_by_pin" in {
      val givenSortBy = "sourceResource.spatial.coordinates"
      val givenSortByPin = "40,-73"
      val params = Map("sort_by" -> givenSortBy, "sort_by_pin" -> givenSortByPin)
      itemParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.sortBy shouldEqual Some(givenSortBy)
      msg.params.sortByPin shouldEqual Some(givenSortByPin)
    }

    "reject sort_by coordinates if sort_by_pin is not and accepted param" in {
      val givenSortBy = "sourceResource.spatial.coordinates"
      val givenSortByPin = "40,-73"
      val params = Map("sort_by" -> givenSortBy, "sort_by_pin" -> givenSortByPin)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "reject sort_by coordinates if sort_by_pin is not present" in {
      val givenSortBy = "sourceResource.spatial.coordinates"
      val params = Map("sort_by" -> givenSortBy)
      itemParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "reject sort_by_pin if sort_by coordinates is not present" in {
      val givenSortBy = "dataProvider.name"
      val givenSortByPin = "40,-73"
      val params = Map("sort_by" -> givenSortBy, "sort_by_pin" -> givenSortByPin)
      itemParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "reject too-long sort_by_pin param" in {
      val givenSortBy = "sourceResource.spatial.coordinates"
      val givenSortByPin = Random.alphanumeric.take(201).mkString
      val params = Map("sort_by" -> givenSortBy, "sort_by_pin" -> givenSortByPin)
      itemParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "reject too-short sort_by_pin param" in {
      val givenSortBy = "sourceResource.spatial.coordinates"
      val givenSortByPin = "4"
      val params = Map("sort_by" -> givenSortBy, "sort_by_pin" -> givenSortByPin)
      itemParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "filter validator" should {
    "accept valid value for URL field" in {
      val given = "provider.@id:http://dp.la/api/contributor/lc"
      val expectedFieldName = Some("provider.@id")
      val expectedValue = Some("http://dp.la/api/contributor/lc")
      val params = Map("filter" -> given)
      itemParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldName = msg.params.filter.map(_.fieldName)
      val value = msg.params.filter.map(_.value)
      assert(fieldName == expectedFieldName)
      assert(value == expectedValue)
    }

    "accept valid value for text field" in {
      val given = "provider.name:california"
      val expectedFieldName = Some("provider.name")
      val expectedValue = Some("california")
      val params = Map("filter" -> given)
      itemParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldName = msg.params.filter.map(_.fieldName)
      val value = msg.params.filter.map(_.value)
      assert(fieldName == expectedFieldName)
      assert(value == expectedValue)
    }

    "reject unsearchable field" in {
      val given = "foo:bar"
      val params = Map("filter" -> given)
      itemParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "reject too-short text value" in {
      val given = "provider.name:b"
      val params = Map("filter" -> given)
      itemParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "reject too-long text value" in {
      val given = "provider.name:" + Random.alphanumeric.take(201).mkString
      val params = Map("filter" -> given)
      itemParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "rejects non-URL value" in {
      val given = "provider.@id:bar"
      val params = Map("filter" -> given)
      itemParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }
}
