package dpla.api.v2.search.mappings

import dpla.api.helpers.FileReader
import dpla.api.v2.search.mappings.DPLAMAPJsonFormats._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json._

class SingleItemMappingTest extends AnyWordSpec with Matchers
  with JsonFieldReader with FileReader {

  val esItem: String = readFile("/elasticSearchItem.json")
  val item: JsObject =
    esItem.parseJson.convertTo[SingleDPLADoc].toJson.asJsObject
  val firstDoc: JsObject = readObjectArray(item,"docs").headOption
    .getOrElse(throw new RuntimeException("Failed to parse 'docs'"))

  "a single item record" should {
    "generate count" in {
      val expected = Some(1)
      val traversed = readInt(item, "count")
      assert(traversed == expected)
    }

    /** Field mappings */

    "map creator" in {
      val expected = "Berndt, Jerry"
      val traversed =
        readStringArray(firstDoc, "sourceResource", "creator")
      traversed should contain only expected
    }

    "map subject" in {
      val expected = Seq(
        "Children",
        "Dwarf hamsters",
        "Saint Vincent Center (Los Angeles)"
      )
      val subjects =
        readObjectArray(firstDoc, "sourceResource", "subject")
      val traversed = subjects.flatMap(s => readString(s, "name"))
      traversed should contain allElementsOf expected
    }

    "map id" in {
      val expected = Some("4d85a6bd965dde8352c8235c43fe1c44")
      val traversed = readString(firstDoc, "id")
      assert(traversed == expected)
    }

    "map isShownAt" in {
      val expected = Some(
        "http://doi.org/10.25549/berndt-m584"
      )
      val traversed =
        readString(firstDoc, "isShownAt")
      assert(traversed == expected)
    }

    "map format" in {
      val expected = "Photographic prints"
      val traversed =
        readStringArray(firstDoc, "sourceResource", "format")
      traversed should contain only expected
    }

    "map object" in {
      val expected = "https://thumbnails.calisphere.org/clip/150x150/a13932ec86f46b89140e6bb915ff1913"
      val traversed = readStringArray(firstDoc, "object")
      traversed should contain only expected
    }

    "map providerName" in {
      val expected = Some("California Digital Library")
      val traversed = readString(firstDoc, "provider", "name")
      assert(traversed === expected)
    }

    "map publisher" in {
      val expected = "University of Southern California. Libraries"
      val traversed =
        readStringArray(firstDoc, "sourceResource", "publisher")
      traversed should contain only expected
    }

    "map displayDate" in {
      val expected = "circa 1996"
      val dates =
        readObjectArray(firstDoc, "sourceResource", "date")
      val traversed = dates.flatMap(s => readString(s, "displayDate"))
      traversed should contain only expected
    }

    "map provider ID" in {
      val expected = Some("http://dp.la/api/contributor/cdl")
      val traversed = readString(firstDoc, "provider", "@id")
      assert(traversed == expected)
    }

    "map description" in {
      val expected = "Photograph of Children playing with hamsters, Saint Vincent Center, Los Angeles, 1996. Three children gather on one side of a table, watching two dwarf hamsters. One hamster is on the table, walking across a piece of paper. The other hamster is on an adult's hands. The three children are focused on the hamster on the table, with one girl reaching for the hamster with her left hand."
      val traversed =
        readStringArray(firstDoc, "sourceResource", "description")
      traversed should contain only expected
    }

    "map title" in {
      val expected = "Children play with hamsters, Saint Vincent Center, Los Angeles, 1996"
      val traversed = readStringArray(firstDoc, "sourceResource", "title")
      traversed should contain only expected
    }
  }
}
