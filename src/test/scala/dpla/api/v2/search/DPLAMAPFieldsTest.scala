package dpla.api.v2.search

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DPLAMAPFieldsTest extends AnyWordSpec with Matchers {

  class DPLAMAPFieldsTester extends DPLAMAPFields
  val tester = new DPLAMAPFieldsTester

  "DplaMapFieldsTest" should {
    "map DPLA MAP fields to ElasticSearch fields" in {
      val dplaFields = Seq(
        "isShownAt",
        "object",
        "provider.@id",
        "provider.name",
        "sourceResource.creator",
        "sourceResource.description",
        "sourceResource.format",
        "sourceResource.language.name",
        "sourceResource.publisher",
        "sourceResource.subject.name",
        "sourceResource.subtitle",
        "sourceResource.title"
      )
      val expected = Seq(
        "isShownAt",
        "object",
        "provider.@id",
        "provider.name",
        "sourceResource.creator",
        "sourceResource.description",
        "sourceResource.format",
        "sourceResource.language.name",
        "sourceResource.publisher",
        "sourceResource.subject.name",
        "sourceResource.subtitle",
        "sourceResource.title"
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
        "sourceResource.description",
        "sourceResource.format",
        "sourceResource.language.name",
        "sourceResource.publisher",
        "sourceResource.subject.name",
        "sourceResource.subtitle",
        "sourceResource.title"
      )
      val expected = Seq(
        "isShownAt",
        "object",
        "provider.@id",
        "provider.name.not_analyzed",
        "sourceResource.description",
        "sourceResource.format.not_analyzed",
        "sourceResource.language.name.not_analyzed",
        "sourceResource.publisher.not_analyzed",
        "sourceResource.subject.name.not_analyzed",
        "sourceResource.subtitle.not_analyzed",
        "sourceResource.title.not_analyzed"
      )
      val mapped = dplaFields.flatMap(tester.getElasticSearchExactMatchField)
      mapped should contain allElementsOf expected
    }
  }
}
