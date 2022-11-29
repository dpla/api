package dpla.api.v2.search

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.api.v2.search.SearchProtocol.{IntermediateSearchResult, SearchCommand}
import dpla.api.v2.search.paramValidators.EbookParamValidator

object MockEbookSearch {

  def apply(
             testKit: ActorTestKit,
             elasticSearchClient: Option[ActorRef[IntermediateSearchResult]] = None,
             mapper: Option[ActorRef[IntermediateSearchResult]] = None
           ): ActorRef[SearchCommand] = {

    object Mock extends SearchBehavior {

      override def spawnMapper(
                                context: ActorContext[SearchCommand]
                              ): ActorRef[IntermediateSearchResult] = {
        mapper.getOrElse(
          context.spawnAnonymous(DPLAMAPMapper())
        )
      }

      override def spawnElasticSearchClient(
                                             context: ActorContext[SearchCommand],
                                             mapper: ActorRef[IntermediateSearchResult]
                                           ): ActorRef[IntermediateSearchResult] = {
        elasticSearchClient.getOrElse(
          context.spawnAnonymous(ElasticSearchClient("fake-endpoint", mapper))
        )
      }

      override def spawnQueryBuilder(
                                      context: ActorContext[SearchCommand],
                                      elasticSearchClient: ActorRef[IntermediateSearchResult]
                                    ): ActorRef[IntermediateSearchResult] =
        context.spawnAnonymous(
          QueryBuilder(elasticSearchClient)
        )

      override def spawnSearchParamValidator(
                                              context: ActorContext[SearchCommand],
                                              queryBuilder: ActorRef[IntermediateSearchResult]
                                            ): ActorRef[IntermediateSearchResult] =
        context.spawnAnonymous(
          EbookParamValidator(queryBuilder)
        )
    }

    testKit.spawn(Mock())
  }
}
