package dpla.ebookapi.v1.ebooks.unit

import dpla.ebookapi.helpers.FileReader
import dpla.ebookapi.v1.ebooks.{JsonFieldReader, SingleEbook}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import dpla.ebookapi.v1.ebooks.JsonFormats._
import spray.json._

class SingleEbookMappingTest extends AnyWordSpec with Matchers with JsonFieldReader with FileReader {

  val esEbook: String = readFile("/elasticSearchEbook.json")
  val ebook: JsObject = esEbook.parseJson.convertTo[SingleEbook].toJson.asJsObject
  val firstDoc: JsObject = readObjectArray(ebook,"docs").headOption
    .getOrElse(throw new RuntimeException("Failed to parse 'docs'"))

  val esMinEbook: String = readFile("/elasticSearchMinimalEbook.json")
  val minEbook: JsObject = esMinEbook.parseJson.convertTo[SingleEbook].toJson.asJsObject
  val minFirstDoc: JsObject = readObjectArray(minEbook,"docs").headOption
    .getOrElse(throw new RuntimeException("Failed to parse 'docs'"))

  "a single ebook record" should {
    "generate count" in {
      val expected = 1
      val traversed = readInt(ebook, "count").getOrElse("NOT FOUND")
      assert(traversed == expected)
    }

    "generate type" in {
      val expected = "ebook"
      val traversed = readString(firstDoc, "sourceResource", "type").getOrElse("NOT FOUND")
      assert(traversed == expected)
    }

    /** Field mappings */

    "map author" in {
      val expected = "J. S. Fletcher"
      val traversed = readStringArray(firstDoc, "sourceResource", "creator")
      traversed should contain only expected
    }

    "map genre" in {
      val expected = Seq(
        "Detective and mystery stories",
        "Murder -- Investigation -- Fiction",
        "Lawyers -- Fiction",
        "London (England) -- Social life and customs -- 20th century -- Fiction"
      )
      val traversed = readStringArray(firstDoc, "sourceResource", "subject", "name")
      traversed should contain allElementsOf expected
    }

    "map id" in {
      val expected = "wfwPJ34Bj-MaVWqX9Kac"
      val traversed = readString(firstDoc, "id").getOrElse("NOT FOUND")
      assert(traversed == expected)
    }

    "map itemUri" in {
      val expected = "https://standardebooks.org/ebooks/j-s-fletcher/the-charing-cross-mystery"
      val traversed = readString(firstDoc, "isShownAt").getOrElse("NOT FOUND")
      assert(traversed == expected)
    }

    "map language" in {
      val expected = "en-GB"
      val traversed = readStringArray(firstDoc, "sourceResource", "language", "name")
      traversed should contain only expected
    }

    "map medium" in {
      val expected = "Medium"
      val traversed = readStringArray(firstDoc, "sourceResource", "format")
      traversed should contain only expected
    }

    "map payloadUri" in {
      val expected = "http://payload-permanent-address.dp.la"
      val traversed = readStringArray(firstDoc, "object")
      traversed should contain only expected
    }

    "map providerName" in {
      val expected = Some("Standard Ebooks")
      val traversed = readString(firstDoc, "provider", "name")
      assert(traversed === expected)
    }

    "map publisher" in {
      val expected = "Publisher"
      val traversed = readStringArray(firstDoc, "sourceResource", "publisher")
      traversed should contain only expected
    }

    "map publicationDate" in {
      val expected = "1922"
      val traversed = readStringArray(firstDoc, "sourceResource", "date", "displayDate")
      traversed should contain only expected
    }

    "map sourceUri" in {
      val expected = "http://standardebooks.org"
      val traversed = readString(firstDoc, "provider", "@id").getOrElse("NOT FOUND")
      assert(traversed == expected)
    }

    "map subtitle" in {
      val expected = "This is a subtitle"
      val traversed = readStringArray(firstDoc, "sourceResource", "subtitle")
      traversed should contain only expected
    }

    "map summary" in {
      val expected = "A young lawyer witnesses a suspicious death on a late-night train and investigates what turns out to be a murder."
      val traversed = readStringArray(firstDoc, "sourceResource", "description")
      traversed should contain only expected
    }

    "map title" in {
      val expected = "The Charing Cross Mystery"
      val traversed = readStringArray(firstDoc, "sourceResource", "title")
      traversed should contain only expected
    }

    /** Handle empty fields */

    "ignore empty author" in {
      val parent = readObject(minFirstDoc, "sourceResource")
      val fieldNames = parent.get.fields.keys
      fieldNames should not contain "creator"
    }

    "ignore empty genre" in {
      val parent = readObject(minFirstDoc, "sourceResource")
      val fieldNames = parent.get.fields.keys
      fieldNames should not contain "subject"
    }

    "ignore empty itemUri" in {
      val parent = minFirstDoc
      val fieldNames = parent.fields.keys
      fieldNames should not contain "isShownAt"
    }

    "ignore empty language" in {
      val parent = readObject(minFirstDoc, "sourceResource")
      val fieldNames = parent.get.fields.keys
      fieldNames should not contain "language"
    }

    "ignore empty medium" in {
      val parent = readObject(minFirstDoc, "sourceResource")
      val fieldNames = parent.get.fields.keys
      fieldNames should not contain "format"
    }

    "ignore empty payloadUri" in {
      val parent = minFirstDoc
      val fieldNames = parent.fields.keys
      fieldNames should not contain "object"
    }

    "ignore empty publisher" in {
      val parent = readObject(minFirstDoc, "sourceResource")
      val fieldNames = parent.get.fields.keys
      fieldNames should not contain "publisher"
    }

    "ignore empty publicationDate" in {
      val parent = readObject(minFirstDoc, "sourceResource")
      val fieldNames = parent.get.fields.keys
      fieldNames should not contain "date"
    }

    "ignore empty sourceUri" in {
      val parent = minFirstDoc
      val fieldNames = parent.fields.keys
      fieldNames should not contain "provider.@id"
    }

    "ignore empty subtitle" in {
      val parent = readObject(minFirstDoc, "sourceResource")
      val fieldNames = parent.get.fields.keys
      fieldNames should not contain "subtitle"
    }

    "ignore empty summary" in {
      val parent = readObject(minFirstDoc, "sourceResource")
      val fieldNames = parent.get.fields.keys
      fieldNames should not contain "description"
    }

    "ignore empty title" in {
      val parent = readObject(minFirstDoc, "sourceResource")
      val fieldNames = parent.get.fields.keys
      fieldNames should not contain "title"
    }
  }
}
