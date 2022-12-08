package dpla.api.v2.search.mappings

import dpla.api.v2.search.mappings.PssJsonFormats._
import dpla.api.v2.search.paramValidators.SearchParams
import spray.json._

import scala.util.Try

/**
 * Maps PSS responses to case classes.
 */

case class SinglePssDoc(
//                         `@id`: Option[String],
//                         `@type`: Option[String],
//                         thumbnailUrl: Option[String],
//                         repImageUrl: Option[String],
//                         name: Option[String],
//                         numberOfItems: Option[String],
//                         about: Seq[JsObject]
                       ) extends MappedResponse

case class PssDocList(
                       `@context`: Option[JsValue],
                       numberOfItems: Option[Int],
                       hasPart: Seq[JsValue],
                       itemListElement: Seq[JsValue]
                     ) extends MappedResponse

object PssMapper extends Mapper {

  override protected def mapDocList(
                                     body: String,
                                     searchParams: Option[SearchParams] = None
                                   ): Try[MappedResponse] =
    Try {
      println(body)
      body.parseJson.convertTo[PssDocList]
    }

  override protected def mapSingleDoc(body: String): Try[MappedResponse] =
    Try {
      body.parseJson.convertTo[SinglePssDoc]
    }
}
