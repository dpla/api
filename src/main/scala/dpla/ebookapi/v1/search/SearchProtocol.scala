package dpla.ebookapi.v1.search

import akka.actor.typed.ActorRef
import spray.json.JsValue

object SearchProtocol {

  /** Public command protocol */
  sealed trait SearchCommand

  final case class Search(
                             rawParams: Map[String, String],
                             replyTo: ActorRef[SearchResponse]
                           ) extends SearchCommand

  final case class Fetch(
                           id: String,
                           rawParams: Map[String, String],
                           replyTo: ActorRef[SearchResponse]
                         ) extends SearchCommand

  /** Public response protocol */
  sealed trait SearchResponse

  final case class EbookSearchResult(
                                      ebookList: EbookList
                                    ) extends SearchResponse

  final case class EbookFetchResult(
                                     singleEbook: SingleEbook
                                   ) extends SearchResponse

  final case class InvalidSearchParams(
                                        message: String
                                      ) extends SearchResponse

  final case object FetchNotFound extends SearchResponse
  final case object SearchFailure extends SearchResponse

  /**
   * Internal command protocol.
   * Used by actors within the search package to communicate with one another.
   */
  private[search] sealed trait IntermediateSearchResult

  private[search] final case class RawSearchParams(
                                                    params: Map[String, String],
                                                    replyTo: ActorRef[SearchResponse]
                                                  ) extends IntermediateSearchResult

  private[search] final case class RawFetchParams(
                                                   id: String,
                                                   params: Map[String, String],
                                                   replyTo: ActorRef[SearchResponse]
                                                 ) extends IntermediateSearchResult

  private[search] final case class ValidSearchParams(
                                                      params: SearchParams,
                                                      replyTo: ActorRef[SearchResponse]
                                                    ) extends IntermediateSearchResult

  private[search] final case class ValidFetchId(
                                                 id: String,
                                                 replyTo: ActorRef[SearchResponse]
                                               ) extends IntermediateSearchResult

  private[search] final case class SearchQuery(
                                                params: SearchParams,
                                                query: JsValue,
                                                replyTo: ActorRef[SearchResponse]
                                              ) extends IntermediateSearchResult

  private[search] final case class SearchQueryResponse(
                                                        params: SearchParams,
                                                        esResponseBody: String,
                                                        replyTo: ActorRef[SearchResponse]
                                                      ) extends IntermediateSearchResult

  private[search] final case class FetchQueryResponse(
                                                       esResponseBody: String,
                                                       replyTo: ActorRef[SearchResponse]
                                                     ) extends IntermediateSearchResult
}
