package dpla.api.v2.search.mappings

import dpla.api.v2.search.mappings.DPLAMAPJsonFormats._
import dpla.api.v2.search.paramValidators.SearchParams
import spray.json._

import scala.util.Try

/**
 * Maps Elastic Search responses to case classes,
 * which can be written to DPLA MAP.
 */

/** Case classes for reading ElasticSearch responses **/
case class SingleDPLADoc(
                          docs: Seq[JsValue]
                        ) extends MappedResponse

case class DPLADocList(
                        count: Option[Int],
                        limit: Option[Int],
                        start: Option[Int],
                        docs: Seq[JsValue],
                        facets: Option[FacetList]
                      ) extends MappedResponse

case class FacetList(
                      facets: Seq[Facet]
                    )

case class Facet(
                  field: String,
                  `type`: String,
                  buckets: Seq[Bucket],
                  bucketsLabel: String
                )

case class Bucket(
                   key: Option[String],
                   keyAsString: Option[String] = None,
                   docCount: Option[Int],
                   from: Option[Int] = None,
                   to: Option[Int] = None
                 )

object DPLAMAPMapper extends Mapper {

  /**
   * If SearchParams is included in the parameters, then pagination data will
   * be added and selected fields will be un-nested.
   *
   * SearchParams should be absent for multi-fetch and random queries.
   */
  override protected def mapSearchResponse(
                                     body: String,
                                     searchParams: Option[SearchParams] = None
                                   ): Try[MappedResponse] =
    Try {
      searchParams match {
        case None =>
          body.parseJson.convertTo[DPLADocList]

        case Some(params) =>
          val start = getStart(params.page, params.pageSize)
          val mapped = body.parseJson.convertTo[DPLADocList]
            .copy(limit=Some(params.pageSize), start=Some(start))
          params.fields match {
            case Some(f) => mapped.copy(docs = unNestFields(mapped.docs, f))
            case None => mapped
          }
      }
    }

  override protected def mapFetchResponse(body: String): Try[MappedResponse] =
    Try {
      body.parseJson.convertTo[SingleDPLADoc]
    }

  /**
   * DPLA MAP field that gives the index of the first result on the page
   * (starting at 1)
   */
  private def getStart(page: Int, pageSize: Int): Int = ((page-1)*pageSize)+1

  private def unNestFields(docs: Seq[JsValue], fields: Seq[String]): Seq[JsValue] = {
    docs.map(doc => {
      var docFields = JsObject()

      fields.foreach(field => {
        val fieldSeq: Seq[String] = field.split("\\.")

        readUnknown(doc.asJsObject, fieldSeq:_*).foreach(json =>
          docFields = JsObject(docFields.fields + (field -> json))
        )
      })

      docFields
    })
  }
}
