package dpla.ebookapi.v1.search

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class EbookFieldsTest extends AnyWordSpec with Matchers {

  class EbookFieldsTester extends EbookFields
  val tester = new EbookFieldsTester

  "DplaMapFieldsTest" should {
    "map DPLA MAP fields to ElasticSearch fields" in {
      val dplaFields = Seq(
        "isShownAt",
        "object",
        "provider.@id",
        "provider.name",
        "sourceResource.creator",
        "sourceResource.date.displayDate",
        "sourceResource.description",
        "sourceResource.format",
        "sourceResource.language.name",
        "sourceResource.publisher",
        "sourceResource.subject.name",
        "sourceResource.subtitle",
        "sourceResource.title"
      )
      val expected = Seq(
        "sourceUri",
        "itemUri",
        "providerName",
        "payloadUri",
        "author",
        "publicationDate",
        "summary",
        "medium",
        "language",
        "publisher",
        "genre",
        "subtitle",
        "title"
      )
      val mapped = dplaFields.flatMap(tester.getElasticSearchField)
      mapped should contain allElementsOf expected
    }

    "map DPLA MAP fields to exact match ElasticSearch fields" in {
      val dplaFields = Seq(
        "isShownAt",
        "object",
        "provider.@id",
        "provider.name",
        "sourceResource.creator",
        "sourceResource.date.displayDate",
        "sourceResource.description",
        "sourceResource.format",
        "sourceResource.language.name",
        "sourceResource.publisher",
        "sourceResource.subject.name",
        "sourceResource.subtitle",
        "sourceResource.title"
      )
      val expected = Seq(
        "sourceUri",
        "itemUri",
        "providerName.not_analyzed",
        "payloadUri",
        "author.not_analyzed",
        "publicationDate.not_analyzed",
        "summary",
        "medium.not_analyzed",
        "language.not_analyzed",
        "publisher.not_analyzed",
        "genre.not_analyzed",
        "subtitle.not_analyzed",
        "title.not_analyzed"
      )
      val mapped = dplaFields.flatMap(tester.getElasticSearchExactMatchField)
      mapped should contain allElementsOf expected
    }
  }
}