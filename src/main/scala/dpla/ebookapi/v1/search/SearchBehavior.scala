package dpla.ebookapi.v1.search

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import dpla.ebookapi.v1.search.SearchProtocol.{Fetch, Search, SearchCommand, IntermediateSearchResult, RawFetchParams, RawSearchParams}

trait SearchBehavior {

  // Abstract methods
  def spawnElasticSearchClient(
                                context: ActorContext[SearchCommand],
                                mapper: ActorRef[IntermediateSearchResult]
                              ): ActorRef[IntermediateSearchResult]

  def spawnMapper(
                   context: ActorContext[SearchCommand]
                 ): ActorRef[IntermediateSearchResult]

  def spawnSearchParamValidator(
                                 context: ActorContext[SearchCommand],
                                 queryBuilder: ActorRef[IntermediateSearchResult],
                                 elasticSearchClient: ActorRef[IntermediateSearchResult]
                               ): ActorRef[IntermediateSearchResult]

  def apply(): Behavior[SearchCommand] = {

    Behaviors.setup[SearchCommand] { context =>

      // Spawn children.
      val mapper: ActorRef[IntermediateSearchResult] =
        spawnMapper(context)

      val elasticSearchClient: ActorRef[IntermediateSearchResult] =
        spawnElasticSearchClient(context, mapper)

      val queryBuilder: ActorRef[IntermediateSearchResult] =
        context.spawn(
          QueryBuilder(elasticSearchClient), "QueryBuilder"
        )

      val searchParamValidator: ActorRef[IntermediateSearchResult] =
        spawnSearchParamValidator(context, queryBuilder, elasticSearchClient)

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
