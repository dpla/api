package dpla.api.v2.search.mappings

import dpla.api.v2.search.models.PssFields
import spray.json._

/**
 * These formats are used for parsing ElasticSearch responses and mapping them
 * to Primary Source Sets output.
 */
object PssJsonFormats extends DefaultJsonProtocol with JsonFieldReader
  with PssFields {

  implicit object PssSetFormat extends RootJsonFormat[PssSet] {
    def read(json: JsValue): PssSet = {
      val root = json.asJsObject
      
      val set: Option[JsObject] = readObjectArray(root, "hits", "hits")
        .headOption
        .flatMap(hit => readObject(hit, "_source"))

      set match {
        case Some(set) =>
          PssSet(
            `@context` = readObject(set, "@context"),
            `@id` = readString(set, "@id"),
            `@type` = readString(set, "@type"),
            `dct:created` = readString(set, "dct:created"),
            `dct:modified` = readString(set, "dct:modified"),
            `dct:type` = readString(set, "dct:type"),
            about = readObjectArray(set, "about"),
            accessibilityControl = readStringArray(set, "accessibilityControl"),
            accessibilityFeature = readStringArray(set, "accessibilityFeature"),
            accessibilityHazard = readStringArray(set, "accessibilityHazard"),
            author = readObjectArray(set, "author"),
            dateCreated = readString(set, "dateCreated"),
            dateModified = readString(set, "dateModified"),
            description = readString(set, "description"),
            educationalAlignment = readObjectArray(set, "educationalAlignment"),
            hasPart = readObjectArray(set, "hasPart"),
            inLanguage = readObjectArray(set, "inLanguage"),
            isRelatedTo = readObjectArray(set, "isRelatedTo"),
            learningResourceType = readString(set, "learningResourceType"),
            license = readString(set, "license"),
            name = readString(set, "name"),
            publisher = readObject(set, "publisher"),
            repImageUrl = readString(set, "repImageUrl"),
            thumbnailUrl = readString(set, "thumbnailUrl"),
            typicalAgeRange = readString(set, "typicalAgeRange")
          )
        case None =>
          throw new RuntimeException("Could not map PSS set.")
      }
    }

    def write(pssSet: PssSet): JsValue = {
      JsObject(
        "@context" -> pssSet.`@context`.toJson,
        "@id" -> pssSet.`@id`.toJson,
        "@type" -> pssSet.`@type`.toJson,
        "dct:dateCreated" -> pssSet.`dct:created`.toJson,
        "dct:dateModified" -> pssSet.`dct:modified`.toJson,
        "dct:type" -> pssSet.`dct:type`.toJson,
        "about" -> pssSet.about.toJson,
        "accessibilityControl" -> pssSet.accessibilityControl.toJson,
        "accessibilityFeature" -> pssSet.accessibilityFeature.toJson,
        "accessibilityHazard" -> pssSet.accessibilityHazard.toJson,
        "author" -> pssSet.author.toJson,
        "dateCreated" -> pssSet.dateCreated.toJson,
        "dateModified" -> pssSet.dateModified.toJson,
        "description" -> pssSet.description.toJson,
        "educationalAlignment" -> pssSet.educationalAlignment.toJson,
        "hasPart" -> pssSet.hasPart.toJson,
        "inLanguage" -> pssSet.inLanguage.toJson,
        "isRelatedTo" -> pssSet.isRelatedTo.toJson,
        "learningResourceType" -> pssSet.learningResourceType.toJson,
        "license" -> pssSet.license.toJson,
        "name" -> pssSet.name.toJson,
        "publisher" -> pssSet.publisher.toJson,
        "repImageUrl" -> pssSet.repImageUrl.toJson,
        "thumbnailUrl" -> pssSet.thumbnailUrl.toJson,
        "typicalAgeRange" -> pssSet.typicalAgeRange.toJson
      ).toJson
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
