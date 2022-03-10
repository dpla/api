package dpla.ebookapi.v1.search

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.ebookapi.v1.search.JsonFormats._
import dpla.ebookapi.v1.search.SearchProtocol.{EbookFetchResult, SearchFailure, EbookSearchResult, IntermediateSearchResult, FetchQueryResponse, SearchQueryResponse}
import spray.json._

import scala.util.{Failure, Success, Try}

/**
 * Maps Elastic Search responses to case classes,
 * which can be written to DPLA MAP.
 */

/** Case classes for reading ElasticSearch responses **/

case class SingleEbook(
                        docs: Seq[Ebook]
                      )

case class EbookList(
                      count: Option[Int],
                      limit: Option[Int],
                      start: Option[Int],
                      docs: Seq[Ebook],
                      facets: Option[FacetList]
                    )

case class Ebook(
                  author: Seq[String] = Seq[String](),
                  genre: Seq[String] = Seq[String](),
                  id: Option[String] = None,
                  itemUri: Option[String] = None,
                  language: Seq[String] = Seq[String](),
                  medium: Seq[String] = Seq[String](),
                  payloadUri: Seq[String] = Seq[String](),
                  providerName: Option[String] = None,
                  publisher: Seq[String] = Seq[String](),
                  publicationDate: Seq[String] = Seq[String](),
                  sourceUri: Option[String] = None,
                  subtitle: Seq[String] = Seq[String](),
                  summary: Seq[String]= Seq[String](),
                  title: Seq[String] = Seq[String]()
                )

case class FacetList(
                      facets: Seq[Facet]
                    )

case class Facet(
                  field: String,
                  buckets: Seq[Bucket]
                )

case class Bucket(
                   key: Option[String],
                   docCount: Option[Int]
                 )

object EbookMapper {

  def apply(): Behavior[IntermediateSearchResult] = {

    Behaviors.setup[IntermediateSearchResult] { context =>

      Behaviors.receiveMessage[IntermediateSearchResult] {

        case SearchQueryResponse(params, body, replyTo) =>
          mapEbookList(params, body) match {
            case Success(ebookList) =>
              replyTo ! EbookSearchResult(ebookList)
            case Failure(e) =>
              context.log.error(
                "Failed to parse EbookList from ElasticSearch response:", e
              )
              replyTo ! SearchFailure
          }
          Behaviors.same

        case FetchQueryResponse(body, replyTo) =>
          mapSingleEbook(body) match {
            case Success(singleEbook) =>
              replyTo ! EbookFetchResult(singleEbook)
            case Failure(e) =>
              context.log.error(
                "Failed to parse SingleEbook from ElasticSearch response:", e
              )
              replyTo ! SearchFailure
          }
          Behaviors.same

        case _ =>
          Behaviors.unhandled
      }
    }
  }

  private def mapEbookList(params: SearchParams, body: String): Try[EbookList] =
    Try {
      val start = getStart(params.page, params.pageSize)
      body.parseJson.convertTo[EbookList]
        .copy(limit=Some(params.pageSize), start=Some(start))
    }

  private def mapSingleEbook(body: String): Try[SingleEbook] =
    Try {
      body.parseJson.convertTo[SingleEbook]
    }

  /**
   * DPLA MAP field that gives the index of the first result on the page
   * (starting at 1)
   */
  private def getStart(page: Int, pageSize: Int): Int = ((page-1)*pageSize)+1
}
