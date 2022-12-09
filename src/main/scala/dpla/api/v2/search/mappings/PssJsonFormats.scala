package dpla.api.v2.search.mappings

import dpla.api.v2.search.models.PssFields
import spray.json._

/**
 * These formats are used for parsing ElasticSearch responses and mapping them
 * to Primary Source Sets output.
 */
object PssJsonFormats extends DefaultJsonProtocol with JsonFieldReader
  with PssFields {

  implicit object SinglePssDocFormat extends RootJsonFormat[PssSet] {
    def read(json: JsValue): PssSet = {
      val root = json.asJsObject

      PssSet()
    }

    def write(singlePssDoc: PssSet): JsValue = {
      JsObject()
    }
  }

  implicit object PssSetListFormat extends RootJsonFormat[PssSetList] {

    def read(json: JsValue): PssSetList = {
      val root = json.asJsObject

      PssSetList(
        `@context` = readObjectArray(root, "hits", "hits").headOption.map { hit =>
          readObject(hit, "_source", "@context").getOrElse(JsObject()).toJson
        },
        numberOfItems = readInt(root, "hits", "total", "value"),
        hasPart = Seq(), // TODO add facets
        itemListElement = readObjectArray(root, "hits", "hits").map {
          hit => readObject(hit, "_source").getOrElse(JsObject()).toJson
        }
      )
    }

    def write(pssDocList: PssSetList): JsValue =
      JsObject(
        "@context" -> pssDocList.`@context`.toJson,
        "@type" -> "ItemList".toJson,
        "url" -> "foo".toJson, // TODO CHANGE ME
        "numberOfItems" -> pssDocList.numberOfItems.toJson,
        "hasPart" -> pssDocList.hasPart.toJson,
        "itemListElement" -> pssDocList.itemListElement.toJson
      ).toJson
  }
}
