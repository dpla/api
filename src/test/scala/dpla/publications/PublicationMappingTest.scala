package dpla.publications

import scala.io.{BufferedSource, Source}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json._
import JsonFormats._

class PublicationMappingTest extends AnyWordSpec with Matchers with JsonFieldReader {

  def readFile(filePath: String): String = {
    val source: String = getClass.getResource(filePath).getPath
    val buffered: BufferedSource = Source.fromFile(source)
    buffered.getLines.mkString
  }

  val data: String = readFile("/elasticSearchPublication.json")
  val mapped: JsObject = data.parseJson.convertTo[Publication].toJson.asJsObject

  "JsonFormats" should {
    "generate count" in {
      val expected = 1
      val traversed = readInt(mapped, "count").getOrElse("NOT FOUND")
      assert(traversed == expected)
    }

    "generate ingestType" in {
      val expected = "ebook"
      val traversed = readString(mapped, "docs", "ingestType").getOrElse("NOT FOUND")
      assert(traversed == expected)
    }

    "generate type" in {
      val expected = "ebook"
      val traversed = readString(mapped, "docs", "sourceResource", "type").getOrElse("NOT FOUND")
      assert(traversed == expected)
    }

    "map author" in {
      val expected = "J. S. Fletcher"
      val traversed = readStringArray(mapped, "docs", "sourceResource", "creator")
      traversed should contain only expected
    }

    "map genre" in {
      val expected = Seq(
        "Detective and mystery stories",
        "Murder -- Investigation -- Fiction",
        "Lawyers -- Fiction",
        "London (England) -- Social life and customs -- 20th century -- Fiction"
      )
      val traversed = readStringArray(mapped, "docs", "sourceResource", "subject", "name")
      traversed should contain allElementsOf expected
    }

    "map id" in {
      val expected = "wfwPJ34Bj-MaVWqX9Kac"
      val traversed = readString(mapped, "docs", "id").getOrElse("NOT FOUND")
      assert(traversed == expected)
    }

    "map itemUri" in {
      val expected = "https://standardebooks.org/ebooks/j-s-fletcher/the-charing-cross-mystery"
      val traversed = readString(mapped, "docs", "isShownAt").getOrElse("NOT FOUND")
      assert(traversed == expected)
    }

    "map language" in {
      val expected = "en-GB"
      val traversed = readStringArray(mapped, "docs", "sourceResource", "language", "name")
      traversed should contain only expected
    }

    "map medium" in {
      val expected = "Medium"
      val traversed = readStringArray(mapped, "docs", "sourceResource", "format")
      traversed should contain only expected
    }

    "map payloadUri" in {
      val expected = "http://payload-permanent-address.dp.la"
      val traversed = readStringArray(mapped, "docs", "object")
      traversed should contain only expected
    }

    "map publisher" in {
      val expected = "Publisher"
      val traversed = readStringArray(mapped, "docs", "sourceResource", "publisher")
      traversed should contain only expected
    }

    "map publicationDate" in {
      val expected = "1922"
      val traversed = readStringArray(mapped, "docs", "sourceResource", "date", "displayDate")
      traversed should contain only expected
    }

    "map sourceUri" in {
      val expected = "http://standardebooks.org"
      val traversed = readString(mapped, "docs", "dataProvider").getOrElse("NOT FOUND")
      assert(traversed == expected)
    }

    "map subtitle" in {
      val expected = "This is a subtitle"
      val traversed = readStringArray(mapped, "docs", "sourceResource", "subtitle")
      traversed should contain only expected
    }

    "map summary" in {
      val expected = "A young lawyer witnesses a suspicious death on a late-night train and investigates what turns out to be a murder."
      val traversed = readStringArray(mapped, "docs", "sourceResource", "description")
      traversed should contain only expected
    }

    "map title" in {
      val expected = "The Charing Cross Mystery"
      val traversed = readStringArray(mapped, "docs", "sourceResource", "title")
      traversed should contain only expected
    }
  }
}
