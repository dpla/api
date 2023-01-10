package dpla.api.v2.search.mappings

import dpla.api.helpers.FileReader
import dpla.api.v2.search.mappings.DPLAMAPJsonFormats._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json._

class SingleEbookMappingTest extends AnyWordSpec with Matchers
  with JsonFieldReader with FileReader {

  val esEbook: String = readFile("/elasticSearchEbook.json")
  val ebook: JsObject =
    esEbook.parseJson.convertTo[SingleDPLADoc].toJson.asJsObject
  val firstDoc: JsObject = readObjectArray(ebook,"docs").headOption
    .getOrElse(throw new RuntimeException("Failed to parse 'docs'"))

  val esMinEbook: String = readFile("/elasticSearchMinimalEbook.json")
  val minEbook: JsObject =
    esMinEbook.parseJson.convertTo[SingleDPLADoc].toJson.asJsObject
  val minFirstDoc: JsObject = readObjectArray(minEbook,"docs").headOption
    .getOrElse(throw new RuntimeException("Failed to parse 'docs'"))

  "a single ebook record" should {
    "generate count" in {
      val expected = Some(1)
      val traversed = readInt(ebook, "count")
      assert(traversed == expected)
    }

    /** Field mappings */

    "map subject" in {
      val expected = Seq(
        "Peace Corps (U.S.)--Posters",
        "Teaching--Aids and devices--Posters"
      )
      val subjects =
        readObjectArray(firstDoc, "sourceResource", "subject")
      val traversed = subjects.flatMap(s => readString(s, "name"))
      traversed should contain allElementsOf expected
    }

    "map id" in {
      val expected = Some("3dbbf125aba2642e21f17c955bef4e96")
      val traversed = readString(firstDoc, "id")
      assert(traversed == expected)
    }

    "map isShownAt" in {
      val expected = Some(
        "http://catalog.gpo.gov/F/?func=direct&doc_number=000637392&format=999"
      )
      val traversed =
        readString(firstDoc, "isShownAt")
      assert(traversed == expected)
    }

    "map format" in {
      val expected = Seq(
        "Nonprojected graphic",
        "Electronic resource"
      )
      val traversed =
        readStringArray(firstDoc, "sourceResource", "format")
      traversed should contain allElementsOf expected
    }

    "map provider.@id" in {
      val expected = Some("http://dp.la/api/contributor/gpo")
      val traversed = readString(firstDoc, "provider", "@id")
      assert(traversed === expected)
    }

    "map provider.name" in {
      val expected = Some("United States Government Publishing Office (GPO)")
      val traversed = readString(firstDoc, "provider", "name")
      assert(traversed === expected)
    }

    "map publisher" in {
      val expected = "[Washington, D.C.] : Paul D. Coverdell Worldwise Schools,"
      val traversed =
        readStringArray(firstDoc, "sourceResource", "publisher")
      traversed should contain only expected
    }

    "map displayDate" in {
      val expected = "[2009?]"
      val dates =
        readObjectArray(firstDoc, "sourceResource", "date")
      val traversed = dates.flatMap(s => readString(s, "displayDate"))
      traversed should contain only expected
    }

    "map description" in {
      val expected = "Title from PDF screen (viewed Apr. 15, 2009)"
      val traversed =
        readStringArray(firstDoc, "sourceResource", "description")
      traversed should contain only expected
    }

    "map title" in {
      val expected = "Inspire curiosity Free! Peace Corps resources for yuur classroom /"
      val traversed = readStringArray(firstDoc, "sourceResource", "title")
      traversed should contain only expected
    }

    /** Handle empty fields */

    "ignore empty creator" in {
      val parent = readObject(minFirstDoc, "sourceResource")
      val fieldNames = parent.get.fields.keys
      fieldNames should not contain "creator"
    }

    "ignore empty subject" in {
      val parent = readObject(minFirstDoc, "sourceResource")
      val fieldNames = parent.get.fields.keys
      fieldNames should not contain "subject"
    }

    "ignore empty isShownAt" in {
      val parent = minFirstDoc
      val fieldNames = parent.fields.keys
      fieldNames should not contain "isShownAt"
    }

    "ignore empty language" in {
      val parent = readObject(minFirstDoc, "sourceResource")
      val fieldNames = parent.get.fields.keys
      fieldNames should not contain "language"
    }

    "ignore empty format" in {
      val parent = readObject(minFirstDoc, "sourceResource")
      val fieldNames = parent.get.fields.keys
      fieldNames should not contain "format"
    }

    "ignore empty object" in {
      val parent = minFirstDoc
      val fieldNames = parent.fields.keys
      fieldNames should not contain "object"
    }

    "ignore empty publisher" in {
      val parent = readObject(minFirstDoc, "sourceResource")
      val fieldNames = parent.get.fields.keys
      fieldNames should not contain "publisher"
    }

    "ignore empty date" in {
      val parent = readObject(minFirstDoc, "sourceResource")
      val fieldNames = parent.get.fields.keys
      fieldNames should not contain "date"
    }

    "ignore empty provider.@id" in {
      val parent = minFirstDoc
      val fieldNames = parent.fields.keys
      fieldNames should not contain "provider.@id"
    }

    "ignore empty subtitle" in {
      val parent = readObject(minFirstDoc, "sourceResource")
      val fieldNames = parent.get.fields.keys
      fieldNames should not contain "subtitle"
    }

    "ignore empty description" in {
      val parent = readObject(minFirstDoc, "sourceResource")
      val fieldNames = parent.get.fields.keys
      fieldNames should not contain "description"
    }
  }
}
