package dpla.api.v2.search.mappings

import dpla.api.v2.search.mappings.PssJsonFormats._
import dpla.api.v2.search.paramValidators.SearchParams
import spray.json._

import scala.util.Try

/**
 * Maps PSS responses to case classes.
 */

case class PssSetList(
                       `@context`: Option[JsValue],
                       numberOfItems: Option[Int],
                       hasPart: Seq[JsValue],
                       itemListElement: Seq[JsValue]
                     ) extends MappedResponse

case class PssSet(
                    `@context`: Option[JsValue],
                    `@id`: Option[String],
                    `@type`: Option[String],
                    `dct:created`: Option[String],
                    `dct:modified`: Option[String],
                    `dct:type`: Option[String],
                    about: Seq[JsValue],
                    accessibilityControl: Seq[String],
                    accessibilityFeature: Seq[String],
                    accessibilityHazard: Seq[String],
                    author: Seq[JsValue],
                    dateCreated: Option[String],
                    dateModified: Option[String],
                    description: Option[String],
                    educationalAlignment: Seq[JsValue],
                    hasPart: Seq[JsValue],
                    inLanguage: Seq[JsValue],
                    isRelatedTo: Seq[JsValue],
                    learningResourceType: Option[String],
                    license: Option[String],
                    name: Option[String],
                    publisher: Option[JsValue],
                    repImageUrl: Option[String],
                    thumbnailUrl: Option[String],
                    typicalAgeRange: Option[String]
                 ) extends MappedResponse

case class PssSource()

object PssMapper extends Mapper {

  override protected def mapDocList(
                                     body: String,
                                     searchParams: Option[SearchParams] = None
                                   ): Try[MappedResponse] = {

    val mappedSetList = Try { body.parseJson.convertTo[PssSetList] }
    val mappedSet = Try { body.parseJson.convertTo[PssSet] }

    searchParams match {
      case Some(params) =>
        val queryFields = params.fieldQueries.map(_.fieldName)
        if (queryFields.contains("@id")) mappedSet
        else mappedSetList
      case None => mappedSetList
    }
  }

  override protected def mapSingleDoc(body: String): Try[MappedResponse] =
    Try {
      body.parseJson.convertTo[PssSet]
    }
}
