package dpla.api.v2.search

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.api.v2.search.JsonFormats._
import dpla.api.v2.search.SearchProtocol.{DPLAMAPFetchResult, DPLAMAPMultiFetchResult, DPLAMAPRandomResult, DPLAMAPSearchResult, FetchQueryResponse, IntermediateSearchResult, MultiFetchQueryResponse, RandomQueryResponse, SearchFailure, SearchQueryResponse}
import spray.json._

import scala.util.{Failure, Success, Try}

/**
 * Maps Elastic Search responses to case classes,
 * which can be written to DPLA MAP.
 */

/** Case classes for reading ElasticSearch responses **/
case class SingleDPLADoc(
                          docs: Seq[JsValue]
                        )

case class DPLADocList(
                        count: Option[Int],
                        limit: Option[Int],
                        start: Option[Int],
                        docs: Seq[JsValue],
                        facets: Option[FacetList]
                      )

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

object DPLAMAPMapper {

  def apply(): Behavior[IntermediateSearchResult] = {

    Behaviors.setup[IntermediateSearchResult] { context =>

      Behaviors.receiveMessage[IntermediateSearchResult] {

        case SearchQueryResponse(params, body, replyTo) =>
          mapDPLADocList(params, body) match {
            case Success(dplaDocList) =>
              replyTo ! DPLAMAPSearchResult(dplaDocList)
            case Failure(e) =>
              context.log.error(
                "Failed to parse DPLADocList from ElasticSearch response:", e
              )
              replyTo ! SearchFailure
          }
          Behaviors.same

        case FetchQueryResponse(body, replyTo) =>
          mapSingleDPLADoc(body) match {
            case Success(singleDPLADoc) =>
              replyTo ! DPLAMAPFetchResult(singleDPLADoc)
            case Failure(e) =>
              context.log.error(
                "Failed to parse SingleDPLADoc from ElasticSearch response:", e
              )
              replyTo ! SearchFailure
          }
          Behaviors.same

        case MultiFetchQueryResponse(body, replyTo) =>
          mapMultiFetch(body) match {
            case Success(multiDPLADoc) =>
              replyTo ! DPLAMAPMultiFetchResult(multiDPLADoc)
            case Failure(e) =>
              context.log.error(
                "Failed to parse DPLADocList from ElasticSearch response:", e
              )
              replyTo ! SearchFailure
          }
          Behaviors.same

        case RandomQueryResponse(_, body, replyTo) =>
          mapRandom(body) match {
            case Success(dplaDocList) =>
              replyTo ! DPLAMAPRandomResult(dplaDocList)
            case Failure(e) =>
              context.log.error(
                "Failed to parse DPLADocList from ElasticSearch response:", e
              )
              replyTo ! SearchFailure
          }
          Behaviors.same

        case _ =>
          Behaviors.unhandled
      }
    }
  }

  private def mapDPLADocList(params: SearchParams, body: String): Try[DPLADocList] =
    Try {
      val start = getStart(params.page, params.pageSize)
      val mapped = body.parseJson.convertTo[DPLADocList]
        .copy(limit=Some(params.pageSize), start=Some(start))
      params.fields match {
        case Some(f) => mapped.copy(docs = unNestFields(mapped.docs, f))
        case None => mapped
      }
    }

  private def mapSingleDPLADoc(body: String): Try[SingleDPLADoc] =
    Try {
      body.parseJson.convertTo[SingleDPLADoc]
    }

  private def mapMultiFetch(body: String): Try[DPLADocList] =
    Try{
      body.parseJson.convertTo[DPLADocList]
    }

  private def mapRandom(body: String): Try[DPLADocList] =
    Try{
      body.parseJson.convertTo[DPLADocList]
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
