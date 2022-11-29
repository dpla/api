package dpla.api.v2.search.models

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PssFieldsTest extends AnyWordSpec with Matchers {

  class PssFieldsTester extends PssFields
  val tester = new PssFieldsTester

  "PssFieldsTest" should {
    "map PSS fields to ElasticSearch fields" in {
      val dataFields = tester.allDataFields
      val expected = tester.fields.map(_.elasticSearchDefault)
      val mapped = dataFields.flatMap(tester.getElasticSearchField)
      mapped should contain allElementsOf expected
    }

    "have exact match for each facetable field" in {
      val exactMatch = tester.fields.filter(_.elasticSearchNotAnalyzed.nonEmpty)
      val facetable = tester.fields.filter(_.facetable)
      exactMatch should contain allElementsOf facetable
    }

    "have exact match for each sortable field" in {
      val exactMatch = tester.fields.filter(_.elasticSearchNotAnalyzed.nonEmpty)
      val sortable = tester.fields.filter(_.sortable)
      exactMatch should contain allElementsOf sortable
    }
  }
}
