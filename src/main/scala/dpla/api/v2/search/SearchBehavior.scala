package dpla.api.v2.search

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import dpla.api.v2.search.SearchProtocol.{Fetch, Search, SearchCommand, IntermediateSearchResult, RawFetchParams, RawSearchParams}

trait SearchBehavior {

  // Abstract methods
  def spawnMapper(
                   context: ActorContext[SearchCommand]
                 ): ActorRef[IntermediateSearchResult]

  def spawnElasticSearchClient(
                                context: ActorContext[SearchCommand],
                                mapper: ActorRef[IntermediateSearchResult]
                              ): ActorRef[IntermediateSearchResult]

  def spawnQueryBuilder(
                         context: ActorContext[SearchCommand],
                         elasticSearchClient: ActorRef[IntermediateSearchResult]
                       ): ActorRef[IntermediateSearchResult]

  def spawnSearchParamValidator(
                                 context: ActorContext[SearchCommand],
                                 queryBuilder: ActorRef[IntermediateSearchResult]
                               ): ActorRef[IntermediateSearchResult]

  def apply(): Behavior[SearchCommand] = {

    Behaviors.setup[SearchCommand] { context =>

      // Spawn children.
      val mapper: ActorRef[IntermediateSearchResult] =
        spawnMapper(context)

      val elasticSearchClient: ActorRef[IntermediateSearchResult] =
        spawnElasticSearchClient(context, mapper)

      val queryBuilder: ActorRef[IntermediateSearchResult] =
        spawnQueryBuilder(context, elasticSearchClient)

      val searchParamValidator: ActorRef[IntermediateSearchResult] =
        spawnSearchParamValidator(context, queryBuilder)

      Behaviors.receiveMessage[SearchCommand] {

        case Search(rawParams, replyTo) =>
          searchParamValidator ! RawSearchParams(rawParams, replyTo)
          Behaviors.same

        case Fetch(id, rawParams, replyTo) =>
          searchParamValidator ! RawFetchParams(id, rawParams, replyTo)
          Behaviors.same

        case _ =>
          Behaviors.unhandled
      }
    }
  }
}
