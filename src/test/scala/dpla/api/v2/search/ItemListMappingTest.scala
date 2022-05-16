package dpla.api.v2.search

import dpla.api.helpers.FileReader
import dpla.api.v2.search.JsonFormats._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json._

class ItemListMappingTest extends AnyWordSpec with Matchers
  with JsonFieldReader with FileReader {

  val esItemList: String =
    readFile("/elasticSearchItemList.json")
  val itemList: JsObject =
    esItemList.parseJson.convertTo[DPLADocList].toJson.asJsObject

  val minEsItemList: String =
    readFile("/elasticSearchMinimalItemList.json")
  val minItemList: JsObject =
    minEsItemList.parseJson.convertTo[DPLADocList].toJson.asJsObject

  "a list of ebook records" should {
    "map count" in {
      val expected = 167
      val traversed = readInt(itemList, "count").get
      assert(traversed == expected)
    }

    "handle empty docs" in {
      val traversed = readObjectArray(minItemList, "docs")
      assert(traversed.isEmpty)
    }

    "map all docs" in {
      val expected = 2
      val docCount = readObjectArray(itemList, "docs").size
      assert(docCount == expected)
    }

    "map doc fields" in {
      val expected = Seq(
        "4d85a6bd965dde8352c8235c43fe1c44",
        "c340ca5de0e46b0538213c65c650c8c6"
      )
      val ids = readObjectArray(itemList, "docs")
        .flatMap(readString(_, "id"))
      ids should contain allElementsOf expected
    }

    "handle empty facets" in {
      val parent = readObject(minItemList)
      val children = parent.get.fields.keys
      children should not contain "facets"
    }

    "map all facets" in {
      val expected = Seq("provider.name")
      val parent = readObject(itemList, "facets")
      val children = parent.get.fields.keys
      children should contain allElementsOf expected
    }

    "map facet type" in {
      val expected = Some("terms")
      val traversed = readString(itemList, "facets", "provider.name", "_type")
      assert(traversed == expected)
    }

    "map facet terms" in {
      val expected = Some("Illinois Digital Heritage Hub")
      val firstTerm =
        readObjectArray(itemList, "facets", "provider.name", "terms").head
      val traversed = readString(firstTerm, "term")
      assert(traversed == expected)
    }

    "map facet counts" in {
      val expected = Some(21)
      val firstTerm =
        readObjectArray(itemList, "facets", "provider.name", "terms").head
      val traversed = readInt(firstTerm, "count")
      assert(traversed == expected)
    }

    "map geo facet type" in {
      val expected = Some("geo_distance")
      val traversed = readString(itemList, "facets",
        "sourceResource.spatial.coordinates", "_type")
      assert(traversed == expected)
    }

    "map geo facet to" in {
      val expected = Some(99)
      val firstTerm =
        readObjectArray(itemList, "facets",
          "sourceResource.spatial.coordinates", "ranges").head
      val traversed = readInt(firstTerm, "to")
      assert(traversed == expected)
    }

    "map geo facet from" in {
      val expected = Some(0)
      val firstTerm =
        readObjectArray(itemList, "facets",
          "sourceResource.spatial.coordinates", "ranges").head
      val traversed = readInt(firstTerm, "from")
      assert(traversed == expected)
    }

    "map geo facet count" in {
      val expected = Some(300156)
      val firstTerm =
        readObjectArray(itemList, "facets",
          "sourceResource.spatial.coordinates", "ranges").head
      val traversed = readInt(firstTerm, "count")
      assert(traversed == expected)
    }
  }
}
