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
    "map the author" in {
      val expected = "J. S. Fletcher"
      val traversed = readStringArray(mapped, "docs", "sourceResource", "creator")
      traversed should contain only expected
    }

    "map the id" in {
      val expected = "wfwPJ34Bj-MaVWqX9Kac"
      val traversed = readString(mapped, "docs", "id").getOrElse("NOT FOUND")
      assert(traversed == expected)
    }
  }
}
