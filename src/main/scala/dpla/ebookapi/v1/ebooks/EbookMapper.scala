package dpla.ebookapi.v1.ebooks

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.ebookapi.v1.ebooks.JsonFormats._
import spray.json._

import scala.util.{Failure, Success, Try}

sealed trait MapperResponse
final case class MapSuccess(response: MappedRecord) extends MapperResponse
final case class MapFailure(message: String) extends MapperResponse

/** Case classes for reading ElasticSearch responses **/

trait MappedRecord

case class SingleEbook(
                        docs: Seq[Ebook]
                      ) extends MappedRecord

case class EbookList(
                      count: Option[Int],
                      limit: Option[Int],
                      start: Option[Int],
                      docs: Seq[Ebook],
                      facets: Option[FacetList]
                    ) extends MappedRecord

case class Ebook(
                  author: Seq[String],
                  genre: Seq[String],
                  id: Option[String],
                  itemUri: Option[String],
                  language: Seq[String],
                  medium: Seq[String],
                  payloadUri: Seq[String],
                  publisher: Seq[String],
                  publicationDate: Seq[String],
                  sourceUri: Option[String],
                  subtitle: Seq[String],
                  summary: Seq[String],
                  title: Seq[String]
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
                                      body: String,
                                      page: Int,
                                      pageSize: Int,
                                      replyTo: ActorRef[MapperResponse]
                                    ) extends MapperCommand

  final case class MapFetchResponse(
                                     body: String,
                                     replyTo: ActorRef[MapperResponse]
                                   ) extends MapperCommand

  def apply(): Behavior[MapperCommand] =
    Behaviors.receiveMessage {
      case MapSearchResponse(body, page, pageSize, replyTo) =>
        replyTo ! mapEbookList(body, page, pageSize)
        Behaviors.same
      case MapFetchResponse(body, replyTo) =>
        replyTo ! mapSingleEbook(body)
        Behaviors.same
    }

  private def mapEbookList(body: String, page: Int, pageSize: Int): MapperResponse =
    Try {
      val start = getStart(page, pageSize)
      body.parseJson.convertTo[EbookList].copy(limit=Some(pageSize), start=Some(start))
    } match {
      case Success(ebookList) =>
        MapSuccess(ebookList)
      case Failure(e) =>
        MapFailure(e.getMessage)
    }

  private def mapSingleEbook(body: String): MapperResponse =
    Try {
      body.parseJson.convertTo[SingleEbook]
    } match {
      case Success(singleEbook) =>
        MapSuccess(singleEbook)
      case Failure(e) =>
        MapFailure(e.getMessage)
    }

  // DPLA MAP field that gives the index of the first result on the page (starting at 1)
  private def getStart(page: Int, pageSize: Int): Int = ((page-1)*pageSize)+1
}
