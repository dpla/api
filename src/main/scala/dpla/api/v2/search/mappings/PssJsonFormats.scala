package dpla.api.v2.search.mappings

import dpla.api.v2.search.models.PssFields
import spray.json._

/**
 * These formats are used for parsing ElasticSearch responses and mapping them
 * to Primary Source Sets output.
 */
object PssJsonFormats extends DefaultJsonProtocol with JsonFieldReader
  with PssFields {

  implicit object SinglePssDocFormat extends RootJsonFormat[SinglePssDoc] {
    def read(json: JsValue): SinglePssDoc = {
      val root = json.asJsObject

      SinglePssDoc()
    }

    def write(singlePssDoc: SinglePssDoc): JsValue = {
      JsObject()
    }
  }

  implicit object PssDocListFormat extends RootJsonFormat[PssDocList] {

    def read(json: JsValue): PssDocList = {
      val root = json.asJsObject

      PssDocList(
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

    def write(pssDocList: PssDocList): JsValue =
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
