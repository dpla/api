package dpla.api.v2.search.mappings

import dpla.api.helpers.FileReader
import dpla.api.v2.search.mappings.PssJsonFormats._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json._

class PssListMappingTest extends AnyWordSpec with Matchers
  with JsonFieldReader with FileReader {

  val esPssList: String =
    readFile("/elasticSearchPssList.json")
  val pssList: JsObject =
    esPssList.parseJson.convertTo[PssSetList].toJson.asJsObject

  "a list of pss records" should {
    "map count" in {
      val expected = 142
      val traversed = readInt(pssList, "numberOfItems").get
      assert(traversed == expected)
    }

    "define @type" in {
      val expected = "ItemList"
      val traversed = readString(pssList, "@type").get
      assert(traversed == expected)
    }

    "map @context" in {
      val expected = "http://schema.org/"
      val traversed = readString(pssList, "@context", "@vocab").get
      assert(traversed == expected)
    }

    "map all sets" in {
      val expected = 10
      val docCount = readObjectArray(pssList, "itemListElement").size
      assert(docCount == expected)
    }

    "map @id for each set" in {
      val expected = "https://baggins.dp.la/primary-source-sets/sets/the-golden-age-of-broadway"
      val traversed = readObjectArray(pssList, "itemListElement").headOption
        .flatMap(set => readString(set, "@id")).get
      assert(traversed == expected)
    }
  }
}