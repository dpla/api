package dpla.ebookapi.v1.search
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.ebookapi.v1.search.SearchProtocol.{SearchCommand, IntermediateSearchResult}

object EbookSearch extends SearchBehavior {

  override def spawnMapper(
                            context: ActorContext[SearchCommand]
                          ): ActorRef[IntermediateSearchResult] =
    context.spawn(EbookMapper(), "EbookMapper")

  override def spawnElasticSearchClient(
                                         context: ActorContext[SearchCommand],
                                         mapper: ActorRef[IntermediateSearchResult]
                                       ): ActorRef[IntermediateSearchResult] = {

    val endpoint: String = context.system.settings.config
      .getString("elasticSearch.ebooksUrl")
      .stripSuffix("/")

    context.spawn(
      ElasticSearchClient(endpoint, mapper), "EbookElasticSearchClient"
    )
  }

  override def spawnSearchParamValidator(
                                          context: ActorContext[SearchCommand],
                                          queryBuilder: ActorRef[IntermediateSearchResult],
                                          elasticSearchClient: ActorRef[IntermediateSearchResult]
                                        ): ActorRef[IntermediateSearchResult] =
    context.spawn(
      EbookParamValidator(queryBuilder, elasticSearchClient), "EbookParamValidator"
    )
}
