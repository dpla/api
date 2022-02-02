package dpla.ebookapi.v1.ebooks

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.unmarshalling._
import dpla.ebookapi.v1.ebooks.JsonFormats._
import spray.json._

import scala.util.{Failure, Success, Try}

sealed trait Parsed
final case class ParseSuccess(response: ElasticSearchResponse) extends Parsed
final case class ParseFailure(message: String) extends Parsed

/** Case classes for reading ElasticSearch responses **/

trait ElasticSearchResponse

case class SingleEbook(
                        docs: Seq[Ebook]
                      ) extends ElasticSearchResponse

case class EbookList(
                      count: Option[Int],
                      limit: Option[Int],
                      start: Option[Int],
                      docs: Seq[Ebook],
                      facets: Option[FacetList]
                    ) extends ElasticSearchResponse

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

object ElasticSearchResponseParser {
  sealed trait ParseCommand

  final case class ParseSearchResponse(
                                        httpResponse: HttpResponse,
                                        replyTo: ActorRef[Parsed]
                                      ) extends ParseCommand

  final case class ParseFetchResponse(
                                        httpResponse: HttpResponse,
                                        replyTo: ActorRef[Parsed]
                                     ) extends ParseCommand

  def apply(): Behavior[ParseCommand] =
    Behaviors.receive { (context, command) =>
      command match {
        case ParseSearchResponse(httpResponse, replyTo) =>
          httpResponse.status.intValue match {
            case 200 =>
              // TODO
              Behaviors.same
            case _ =>



          val parsed = search(params)

          // Map the Future value to a message, handled by this actor
          context.pipeToSelf(futureResponse) {
            case Success(response) =>
              WrappedResponse(EsSuccess(response), replyTo)
            case Failure(e) =>
              WrappedResponse(EsFailure(e.getMessage), replyTo)
          }

          Behaviors.same

}
