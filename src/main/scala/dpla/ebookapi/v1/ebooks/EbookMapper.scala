package dpla.ebookapi.v1.ebooks

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import dpla.ebookapi.v1.ebooks.JsonFormats._
import spray.json._

import scala.util.{Failure, Success, Try}

/**
 * Maps Elastic Search responses to case classes,
 * which can be written to DPLA MAP.
 */

sealed trait EbookMapperResponse

final case class MappedEbookList(
                                  response: EbookList
                                ) extends EbookMapperResponse

final case class MappedSingleEbook(
                                    response: SingleEbook
                                  ) extends EbookMapperResponse

case object MapFailure extends EbookMapperResponse

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
  sealed trait MapperCommand

  final case class MapSearchResponse(
                                      esResponseBody: String,
                                      page: Int,
                                      pageSize: Int,
                                      replyTo: ActorRef[EbookMapperResponse]
                                    ) extends MapperCommand

  final case class MapFetchResponse(
                                     esResponseBody: String,
                                     replyTo: ActorRef[EbookMapperResponse]
                                   ) extends MapperCommand

  def apply(): Behavior[MapperCommand] = {
    Behaviors.setup[MapperCommand] { context =>

      Behaviors.receiveMessage[MapperCommand] {
        case MapSearchResponse(body, page, pageSize, replyTo) =>

          val response: EbookMapperResponse =
            mapEbookList(body, page, pageSize) match {
              case Success(ebookList) =>
                MappedEbookList(ebookList)
              case Failure(e) =>
                context.log.error(
                  "Failed to parse EbookList from ElasticSearch response:", e
                )
                MapFailure
            }
          replyTo ! response
          Behaviors.same

        case MapFetchResponse(body, replyTo) =>

          val response: EbookMapperResponse =
            mapSingleEbook(body) match {
              case Success(singleEbook) =>
                MappedSingleEbook(singleEbook)
              case Failure(e) =>
                context.log.error(
                  "Failed to parse SingleEbook from ElasticSearch response:", e
                )
                MapFailure
            }
          replyTo ! response
          Behaviors.same
      }
    }
  }

  private def mapEbookList(body: String, page: Int, pageSize: Int): Try[EbookList] =
    Try {
      val start = getStart(page, pageSize)
      body.parseJson.convertTo[EbookList]
        .copy(limit=Some(pageSize), start=Some(start))
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
