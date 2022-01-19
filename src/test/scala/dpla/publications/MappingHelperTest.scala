package dpla.publications

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class MappingHelperTest extends AnyWordSpec with Matchers {

  object Tester extends MappingHelper

  "MappingHelper" should {
    "map RawParams fields to DPLA MAP fields" in {
      val rawParamsFields = Seq(
        "creator",
        "dataProvider",
        "date",
        "description",
        "format",
        "isShownAt",
        "language",
        "object",
        "publisher",
        "subject",
        "subtitle",
        "title"
      )

      val expected = Seq(
        "dataProvider",
        "isShownAt",
        "object",
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
      val mapped = rawParamsFields.map(Tester.rawParamToDpla)
      mapped should contain allElementsOf expected
    }

    "map DPLA MAP fields to ElasticSearch fields" in {
      val dplaFields = Seq(
        "dataProvider",
        "isShownAt",
        "object",
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
      val mapped = dplaFields.map(Tester.dplaToElasticSearch)
      mapped should contain allElementsOf expected
    }

    "map DPLA MAP fields to non-analyzed ElasticSearch fields" in {
      val dplaFields = Seq(
        "dataProvider",
        "sourceResource.creator",
        "sourceResource.date.displayDate",
        "sourceResource.format",
        "sourceResource.language.name",
        "sourceResource.publisher",
        "sourceResource.subject.name",
        "sourceResource.subtitle",
        "sourceResource.title"
      )
      val expected = Seq(
        "sourceUri",
        "author.not_analyzed",
        "publicationDate.not_analyzed",
        "medium.not_analyzed",
        "language.not_analyzed",
        "publisher.not_analyzed",
        "genre.not_analyzed",
        "subtitle.not_analyzed",
        "title.not_analyzed"
      )
      val mapped = dplaFields.map(Tester.dplaToElasticSearchExactMatch)
      mapped should contain allElementsOf expected
    }

    "map ElasticSearch fields to DPLA MAP fields" in {
      val esFields = Seq(
        "sourceUri",
        "itemUri",
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

      val expected = Seq(
        "dataProvider",
        "isShownAt",
        "object",
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

      val mapped = esFields.map(Tester.elasticSearchToDpla)
      mapped should contain allElementsOf expected
    }

    "map facetable fields" in {
      val expected = Seq(
        "dataProvider",
        "sourceResource.creator",
        "sourceResource.date.displayDate",
        "sourceResource.format",
        "sourceResource.language.name",
        "sourceResource.publisher",
        "sourceResource.subject.name",
        "sourceResource.subtitle",
        "sourceResource.title"
      )

      val mapped = Tester.facetableDplaFields
      mapped should contain allElementsOf expected
    }
  }
}
