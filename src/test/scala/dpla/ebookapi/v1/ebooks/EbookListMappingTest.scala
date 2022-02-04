package dpla.ebookapi.v1.ebooks

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json._
import JsonFormats._
import dpla.ebookapi.helpers.FileReader

class EbookListMappingTest extends AnyWordSpec with Matchers with JsonFieldReader with FileReader {

  val esEbookList: String = readFile("/elasticSearchEbookList.json")
  val ebookList: JsObject = esEbookList.parseJson.convertTo[EbookList].toJson.asJsObject

  val minEsEbookList: String = readFile("/elasticSearchMinimalEbookList.json")
  val minEbookList: JsObject = minEsEbookList.parseJson.convertTo[EbookList].toJson.asJsObject

  "a list of ebook records" should {
    "map count" in {
      val expected = 590
      val traversed = readInt(ebookList, "count").getOrElse("NOT FOUND")
      assert(traversed == expected)
    }

    "handle empty docs" in {
      val traversed = readObjectArray(minEbookList, "docs")
      assert(traversed.isEmpty)
    }

    "map all docs" in {
      val expected = 10
      val docCount = readObjectArray(ebookList, "docs").size
      assert(docCount == expected)
    }

    "map doc fields" in {
      val expected = Seq(
        "ufwPJ34Bj-MaVWqX9KZL",
        "uvwPJ34Bj-MaVWqX9KZL",
        "u_wPJ34Bj-MaVWqX9KZZ",
        "vPwPJ34Bj-MaVWqX9KZZ",
        "vfwPJ34Bj-MaVWqX9KZZ",
        "vvwPJ34Bj-MaVWqX9KZZ",
        "v_wPJ34Bj-MaVWqX9Kac",
        "wPwPJ34Bj-MaVWqX9Kac",
        "wfwPJ34Bj-MaVWqX9Kac",
        "wvwPJ34Bj-MaVWqX9Kac"
      )
      val ids = readObjectArray(ebookList, "docs").flatMap(readString(_, "id"))
      ids should contain allElementsOf expected
    }

    "handle empty facets" in {
      val parent = readObject(minEbookList)
      val children = parent.get.fields.keys
      children should not contain "facets"
    }

    "map all facets" in {
      val expected = Seq("provider.@id", "sourceResource.creator")
      val parent = readObject(ebookList, "facets")
      val children = parent.get.fields.keys
      children should contain allElementsOf expected
    }

    "map facet terms" in {
      val expected = Some("http://standardebooks.org")
      val firstTerm = readObjectArray(ebookList, "facets", "provider.@id", "terms").head
      val traversed = readString(firstTerm, "term")
      assert(traversed == expected)
    }

    "map facet counts" in {
      val expected = Some(590)
      val firstTerm = readObjectArray(ebookList, "facets", "provider.@id", "terms").head
      val traversed = readInt(firstTerm, "count")
      assert(traversed == expected)
    }
  }
}
