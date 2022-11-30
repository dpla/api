package dpla.api.v2.search.mappings

import dpla.api.v2.search.mappings.PssJsonFormats._
import dpla.api.v2.search.paramValidators.SearchParams
import spray.json._

import scala.util.Try

/**
 * Maps PSS responses to case classes.
 */

case class SinglePssDoc(
                         // TODO
                       ) extends SingleMappedDoc

case class PssDocList(
                       `@context`: Option[JsValue],
                       numberOfItems: Option[Int],
                       hasPart: Seq[JsValue],
                       itemListElement: Seq[JsValue]
                     ) extends MappedDocList

object PssMapper extends Mapper {

  override protected def mapDocList(
                                     body: String,
                                     searchParams: Option[SearchParams] = None
                                   ): Try[MappedDocList] =
    Try {
      body.parseJson.convertTo[PssDocList]
    }

  override protected def mapSingleDoc(body: String): Try[SingleMappedDoc] =
    Try {
      body.parseJson.convertTo[SinglePssDoc]
    }
}
