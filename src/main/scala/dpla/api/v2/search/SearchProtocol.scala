package dpla.api.v2.search

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

  final case class Random(
                           rawParams: Map[String, String],
                           replyTo: ActorRef[SearchResponse],
                         ) extends SearchCommand

  /** Public response protocol */
  sealed trait SearchResponse

  final case class DPLAMAPSearchResult(
                                        dplaDocList: DPLADocList
                                      ) extends SearchResponse

  final case class DPLAMAPFetchResult(
                                       singleDPLADoc: SingleDPLADoc
                                     ) extends SearchResponse

  final case class DPLAMAPMultiFetchResult(
                                            dplaDocList: DPLADocList
                                          ) extends SearchResponse

  final case class DPLAMAPRandomResult(
                                        dplaDocList: DPLADocList
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

  private[search] final case class RawRandomParams(
                                                    params: Map[String, String],
                                                    replyTo: ActorRef[SearchResponse]
                                                  ) extends IntermediateSearchResult

  private[search] final case class ValidSearchParams(
                                                      params: SearchParams,
                                                      replyTo: ActorRef[SearchResponse]
                                                    ) extends IntermediateSearchResult

  private[search] final case class ValidFetchIds(
                                                  ids: Seq[String],
                                                  replyTo: ActorRef[SearchResponse]
                                                ) extends IntermediateSearchResult

  private[search] final case class ValidRandomParams(
                                                      params: RandomParams,
                                                      replyTo: ActorRef[SearchResponse]
                                                    ) extends IntermediateSearchResult

  private[search] final case class SearchQuery(
                                                params: SearchParams,
                                                query: JsValue,
                                                replyTo: ActorRef[SearchResponse]
                                              ) extends IntermediateSearchResult

  private[search] final case class FetchQuery(
                                               id: String,
                                               replyTo: ActorRef[SearchResponse]
                                             ) extends IntermediateSearchResult

  private[search] final case class MultiFetchQuery(
                                                    query: JsValue,
                                                    replyTo: ActorRef[SearchResponse]
                                                  ) extends IntermediateSearchResult

  private[search] final case class RandomQuery(
                                                params: RandomParams,
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
                                                     
  private[search] final case class MultiFetchQueryResponse(
                                                            esResponseBody: String,
                                                            replyTo: ActorRef[SearchResponse]
                                                          ) extends IntermediateSearchResult

  private[search] final case class RandomQueryResponse(
                                                        params: RandomParams,
                                                        esResponseBody: String,
                                                        replyTo: ActorRef[SearchResponse]
                                                      ) extends IntermediateSearchResult
}
