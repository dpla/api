package dpla.api.v2.search

import dpla.api.helpers.FileReader
import dpla.api.v2.search.JsonFormats._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json._

class EbookListMappingTest extends AnyWordSpec with Matchers
  with JsonFieldReader with FileReader {

  // TODO rewrite with new elastic search format

//  val esEbookList: String =
//    readFile("/elasticSearchEbookList.json")
//  val ebookList: JsObject =
//    esEbookList.parseJson.convertTo[EbookList].toJson.asJsObject
//
//  val minEsEbookList: String =
//    readFile("/elasticSearchMinimalEbookList.json")
//  val minEbookList: JsObject =
//    minEsEbookList.parseJson.convertTo[EbookList].toJson.asJsObject
//
//  "a list of ebook records" should {
//    "map count" in {
//      val expected = 590
//      val traversed = readInt(ebookList, "count").getOrElse("NOT FOUND")
//      assert(traversed == expected)
//    }
//
//    "handle empty docs" in {
//      val traversed = readObjectArray(minEbookList, "docs")
//      assert(traversed.isEmpty)
//    }
//
//    "map all docs" in {
//      val expected = 10
//      val docCount = readObjectArray(ebookList, "docs").size
//      assert(docCount == expected)
//    }
//
//    "map doc fields" in {
//      val expected = Seq(
//        "ufwPJ34BjqMaVWqX9KZL",
//        "uvwPJ34BjqMaVWqX9KZL",
//        "uqwPJ34BjqMaVWqX9KZZ",
//        "vPwPJ34BjqMaVWqX9KZZ",
//        "vfwPJ34BjqMaVWqX9KZZ",
//        "vvwPJ34BjqMaVWqX9KZZ",
//        "vqwPJ34BjqMaVWqX9Kac",
//        "wPwPJ34BjqMaVWqX9Kac",
//        "wfwPJ34BjqMaVWqX9Kac",
//        "wvwPJ34BjqMaVWqX9Kac"
//      )
//      val ids = readObjectArray(ebookList, "docs")
//        .flatMap(readString(_, "id"))
//      ids should contain allElementsOf expected
//    }
//
//    "handle empty facets" in {
//      val parent = readObject(minEbookList)
//      val children = parent.get.fields.keys
//      children should not contain "facets"
//    }
//
//    "map all facets" in {
//      val expected = Seq("provider.@id", "sourceResource.creator")
//      val parent = readObject(ebookList, "facets")
//      val children = parent.get.fields.keys
//      children should contain allElementsOf expected
//    }
//
//    "map facet terms" in {
//      val expected = Some("http://standardebooks.org")
//      val firstTerm =
//        readObjectArray(ebookList, "facets", "provider.@id", "terms").head
//      val traversed = readString(firstTerm, "term")
//      assert(traversed == expected)
//    }
//
//    "map facet counts" in {
//      val expected = Some(590)
//      val firstTerm =
//        readObjectArray(ebookList, "facets", "provider.@id", "terms").head
//      val traversed = readInt(firstTerm, "count")
//      assert(traversed == expected)
//    }
//  }
}
