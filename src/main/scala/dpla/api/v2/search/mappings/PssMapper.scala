package dpla.api.v2.search.mappings

import dpla.api.v2.search.mappings.PssJsonFormats._
import dpla.api.v2.search.paramValidators.SearchParams
import spray.json._

import scala.util.Try

/**
 * Maps PSS responses to case classes.
 */

final case class PssSetList(
                       `@context`: Option[JsValue],
                       numberOfItems: Option[Int],
                       hasPart: Seq[JsValue],
                       itemListElement: Seq[JsValue]
                     ) extends MappedResponse

final case class PssSet(
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
                         hasPart: Seq[PssPart],
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

final case class PssPart(
                          `@context`: Option[JsValue],
                          `@id`: Option[String],
                          `@type`: Option[String],
                          `dct:created`: Option[String],
                          `dct:modified`: Option[String],
                          author: Seq[JsValue],
                          dateCreated: Option[String],
                          dateModified: Option[String],
                          disambiguatingDescription: Option[String],
                          hasPart: Seq[JsValue],
                          isPartOf: Option[JsValue],
                          isRelatedTo: Seq[JsValue],
                          mainEntity: Seq[JsValue],
                          name: Option[String],
                          repImageUrl: Option[String],
                          text: Option[String],
                          thumbnailUrl: Option[String]
                        ) extends MappedResponse

object PssMapper extends Mapper {

  override protected def mapSearchResponse(
                                     body: String,
                                     searchParams: Option[SearchParams] = None
                                   ): Try[MappedResponse] = {

    lazy val mappedSetList = Try { body.parseJson.convertTo[PssSetList] }
    lazy val mappedSet = Try { body.parseJson.convertTo[PssSet] }

    searchParams match {
      case Some(params) =>
        val setId: Option[String] =
          params.fieldQueries.find(_.fieldName == "id").map(_.value)

        val sourceId: Option[String] =
          params.fieldQueries.find(_.fieldName == "hasPart.id").map(_.value)

        if (setId.nonEmpty) mappedSet
        else if (sourceId.nonEmpty)
          mappedSet.map(set => {
            set.hasPart.filter(part =>
              part.disambiguatingDescription.contains("source") &&
                part.`@id`.getOrElse("").endsWith("/" + sourceId.getOrElse(""))
            ).head.copy(`@context` = set.`@context`, disambiguatingDescription = None)
          })
        else mappedSetList

      case None => mappedSetList
    }
  }

  override protected def mapFetchResponse(body: String): Try[MappedResponse] =
    Try{ throw new RuntimeException("Map fetch response not implemented for PSS") }
}
