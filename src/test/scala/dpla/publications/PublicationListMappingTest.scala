package dpla.publications

import scala.io.{BufferedSource, Source}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json._
import JsonFormats._

class PublicationListMappingTest extends AnyWordSpec with Matchers with JsonFieldReader {

  def readFile(filePath: String): String = {
    val source: String = getClass.getResource(filePath).getPath
    val buffered: BufferedSource = Source.fromFile(source)
    buffered.getLines.mkString
  }

  val esPubList: String = readFile("/elasticSearchPublicationList.json")
  val pubList: JsObject = esPubList.parseJson.convertTo[PublicationList].toJson.asJsObject

  "a list of ebook records" should {
    "map count" in {
      val expected = 590
      val traversed = readInt(pubList, "count").getOrElse("NOT FOUND")
      assert(traversed == expected)
    }

    "map all docs" in {
      val expected = 10
      val docCount = readObjectArray(pubList, "docs").size
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
      val ids = readObjectArray(pubList, "docs").flatMap(readString(_, "id"))
      ids should contain allElementsOf expected
    }
  }
}
