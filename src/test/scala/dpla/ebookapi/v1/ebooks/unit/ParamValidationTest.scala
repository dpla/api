package dpla.ebookapi.v1.ebooks.unit

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import dpla.ebookapi.v1.ebooks.ParamValidator.{ValidateFetchParams, ValidateSearchParams, ValidationRequest}
import dpla.ebookapi.v1.ebooks.{ParamValidator, ValidFetchParams, ValidSearchParams, InvalidParams, ValidationResponse}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.Random


class ParamValidationTest extends AnyWordSpec with Matchers with BeforeAndAfterAll {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit()

  val paramValidator: ActorRef[ValidationRequest] = testKit.spawn(ParamValidator())
  val probe: TestProbe[ValidationResponse] = testKit.createTestProbe[ValidationResponse]

  "search param validator" should {
    "reject unrecognized params" in {
      val params = Map("foo" -> "bar")
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "fetch param validator" should {
    "reject unrecognized params" in {
      val id = "R0VfVX4BfY91SSpFGqxt"
      val params = Map("foo" -> "bar")
      paramValidator ! ValidateFetchParams(id, params, probe.ref)
      probe.expectMessageType[InvalidParams]
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
      probe.expectMessageType[InvalidParams]
    }

    "reject too-long ID" in {
      val id = "asdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdf"
      paramValidator ! ValidateFetchParams(id, Map(), probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "creator validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(Map(), probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.filters.find(_.fieldName == "sourceResource.creator") shouldEqual expected
    }

    "accept valid param" in {
      val given = "Jules Verne"
      val expected = Some("Jules Verne")
      val params = Map("sourceResource.creator" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.filters.find(_.fieldName == "sourceResource.creator").map(_.value) shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = Map("sourceResource.creator" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.creator" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "provider id validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(Map(), probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.filters.find(_.fieldName == "provider.@id") shouldEqual expected
    }

    "accept valid param" in {
      val given = "https://standardebooks.org"
      val expected = Some("https://standardebooks.org")
      val params = Map("provider.@id" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.filters.find(_.fieldName == "provider.@id").map(_.value) shouldEqual expected
    }

    "reject invalid URL" in {
      val given = "standardebooks"
      val params = Map("provider.@id" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "date validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(Map(), probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.filters.find(_.fieldName == "sourceResource.date.displayDate") shouldEqual expected
    }

    "accept valid param" in {
      val given = "2002"
      val expected = Some("2002")
      val params = Map("sourceResource.date.displayDate" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.filters.find(_.fieldName == "sourceResource.date.displayDate").map(_.value) shouldEqual expected
    }

    "reject too-short param" in {
      val given = "1"
      val params = Map("sourceResource.date.displayDate" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.date.displayDate" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "description validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(Map(), probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.filters.find(_.fieldName == "sourceResource.description") shouldEqual expected
    }

    "accept valid param" in {
      val given = "dogs"
      val expected = Some("dogs")
      val params = Map("sourceResource.description" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.filters.find(_.fieldName == "sourceResource.description").map(_.value) shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = Map("sourceResource.description" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.description" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
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
      probe.expectMessageType[InvalidParams]
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
      probe.expectMessageType[InvalidParams]
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
      probe.expectMessageType[InvalidParams]
    }

    "reject out-of-range param" in {
      val given = "9999"
      val params = Map("facet_size" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
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
      probe.expectMessageType[InvalidParams]
    }
  }

  "format validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(Map(), probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.filters.find(_.fieldName == "sourceResource.format") shouldEqual expected
    }

    "accept valid param" in {
      val given = "article"
      val expected = Some("article")
      val params = Map("sourceResource.format" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.filters.find(_.fieldName == "sourceResource.format").map(_.value) shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = Map("sourceResource.format" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.format" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "isShownAt validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(Map(), probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.filters.find(_.fieldName == "isShownAt") shouldEqual expected
    }

    "accept valid param" in {
      val given = "\"https://standardebooks.org/ebooks/j-s-fletcher/the-charing-cross-mystery\""
      val expected = Some("\"https://standardebooks.org/ebooks/j-s-fletcher/the-charing-cross-mystery\"")
      val params = Map("isShownAt" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.filters.find(_.fieldName == "isShownAt").map(_.value) shouldEqual expected
    }

    "reject invalid URL" in {
      val given = "the-charing-cross-mystery"
      val params = Map("isShownAt" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "language validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(Map(), probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.filters.find(_.fieldName == "sourceResource.language.name") shouldEqual expected
    }

    "accept valid param" in {
      val given = "fr"
      val expected = Some("fr")
      val params = Map("sourceResource.language.name" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.filters.find(_.fieldName == "sourceResource.language.name").map(_.value) shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = Map("sourceResource.language.name" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.language.name" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "object validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(Map(), probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.filters.find(_.fieldName == "object") shouldEqual expected
    }

    "accept valid param" in {
      val given = "http://payload-permanent-address.dp.la"
      val expected = Some("http://payload-permanent-address.dp.la")
      val params = Map("object" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.filters.find(_.fieldName == "object").map(_.value) shouldEqual expected
    }

    "reject invalid URL" in {
      val given = "http/payload-permanent-address.dp.la"
      val params = Map("object" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
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
      probe.expectMessageType[InvalidParams]
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
      probe.expectMessageType[InvalidParams]
    }

    "reject out-of-range param" in {
      val given = "0"
      val params = Map("page" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
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
      probe.expectMessageType[InvalidParams]
    }

    "reject out-of-range param" in {
      val given = "999999"
      val params = Map("page_size" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "publisher validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(Map(), probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.filters.find(_.fieldName == "sourceResource.publisher") shouldEqual expected
    }

    "accept valid param" in {
      val given = "Penguin"
      val expected = Some("Penguin")
      val params = Map("sourceResource.publisher" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.filters.find(_.fieldName == "sourceResource.publisher").map(_.value) shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = Map("sourceResource.publisher" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.publisher" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
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
      probe.expectMessageType[InvalidParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("q" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "subject validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(Map(), probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.filters.find(_.fieldName == "sourceResource.subject.name") shouldEqual expected
    }

    "accept valid param" in {
      val given = "dogs"
      val expected = Some("dogs")
      val params = Map("sourceResource.subject.name" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.filters.find(_.fieldName == "sourceResource.subject.name").map(_.value) shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = Map("sourceResource.subject.name" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.subject.name" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "subtitle validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(Map(), probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.filters.find(_.fieldName == "sourceResource.subtitle") shouldEqual expected
    }

    "accept valid param" in {
      val given = "A play in three acts"
      val expected = Some("A play in three acts")
      val params = Map("sourceResource.subtitle" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.filters.find(_.fieldName == "sourceResource.subtitle").map(_.value) shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = Map("sourceResource.subtitle" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.subtitle" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }

  "title validator" should {
    "handle empty param" in {
      val expected = None
      paramValidator ! ValidateSearchParams(Map(), probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.filters.find(_.fieldName == "sourceResource.title") shouldEqual expected
    }

    "accept valid param" in {
      val given = "The Scarlet Letter"
      val expected = Some("The Scarlet Letter")
      val params = Map("sourceResource.title" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      val msg = probe.expectMessageType[ValidSearchParams]
      msg.searchParams.filters.find(_.fieldName == "sourceResource.title").map(_.value) shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = Map("sourceResource.title" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.title" -> given)
      paramValidator ! ValidateSearchParams(params, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }
}
