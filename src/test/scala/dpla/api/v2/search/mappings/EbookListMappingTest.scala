package dpla.api.v2.search.mappings

import dpla.api.helpers.FileReader
import dpla.api.v2.search.mappings.DPLAMAPJsonFormats._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json._

class EbookListMappingTest extends AnyWordSpec with Matchers
  with JsonFieldReader with FileReader {

  val esEbookList: String =
    readFile("/elasticSearchEbookList.json")
  val ebookList: JsObject =
    esEbookList.parseJson.convertTo[DPLADocList].toJson.asJsObject

  val minEsEbookList: String =
    readFile("/elasticSearchMinimalEbookList.json")
  val minEbookList: JsObject =
    minEsEbookList.parseJson.convertTo[DPLADocList].toJson.asJsObject

  "a list of ebook records" should {
    "map count" in {
      val expected = 248887
      val traversed = readInt(ebookList, "count").get
      assert(traversed == expected)
    }

    "handle empty docs" in {
      val traversed = readObjectArray(minEbookList, "docs")
      assert(traversed.isEmpty)
    }

    "map all docs" in {
      val expected = 2
      val docCount = readObjectArray(ebookList, "docs").size
      assert(docCount == expected)
    }

    "map doc fields" in {
      val expected = Seq(
        "3dbbf125aba2642e21f17c955bef4e96",
        "ccde9a5246356b1048873f9d9c71e5bb"
      )
      val ids = readObjectArray(ebookList, "docs")
        .flatMap(readString(_, "id"))
      ids should contain allElementsOf expected
    }

    "handle empty facets" in {
      val traversed = readObjectArray(minEbookList, "facets")
      traversed shouldBe empty
    }

    "map all facets" in {
      val expected = Seq("provider.@id", "sourceResource.creator")
      val parent = readObject(ebookList, "facets")
      val children = parent.get.fields.keys
      children should contain allElementsOf expected
    }

    "map facet terms" in {
      val expected = Some("http://dp.la/api/contributor/gpo")
      val firstTerm =
        readObjectArray(ebookList, "facets", "provider.@id", "terms").head
      val traversed = readString(firstTerm, "term")
      assert(traversed == expected)
    }

    "map facet counts" in {
      val expected = Some(248887)
      val firstTerm =
        readObjectArray(ebookList, "facets", "provider.@id", "terms").head
      val traversed = readInt(firstTerm, "count")
      assert(traversed == expected)
    }
  }
}
