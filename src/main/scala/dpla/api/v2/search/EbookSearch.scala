package dpla.api.v2.search
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.api.v2.search.SearchProtocol.{IntermediateSearchResult, SearchCommand}
import dpla.api.v2.search.mappings.DPLAMAPMapper
import dpla.api.v2.search.paramValidators.EbookParamValidator

/**
 * Handles control flow for conducting ebook searches and fetches.
 * Public interface for the package.
 */
object EbookSearch extends SearchBehavior {

  override def spawnMapper(
                            context: ActorContext[SearchCommand]
                          ): ActorRef[IntermediateSearchResult] =
    context.spawn(DPLAMAPMapper(), "EbookMapper")

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

  override def spawnQueryBuilder(
                                  context: ActorContext[SearchCommand],
                                  elasticSearchClient: ActorRef[IntermediateSearchResult]
                                ): ActorRef[IntermediateSearchResult] =
    context.spawn(
      QueryBuilder(elasticSearchClient), "EbookQueryBuilder"
    )

  override def spawnSearchParamValidator(
                                          context: ActorContext[SearchCommand],
                                          queryBuilder: ActorRef[IntermediateSearchResult]
                                        ): ActorRef[IntermediateSearchResult] =
    context.spawn(
      EbookParamValidator(queryBuilder), "EbookParamValidator"
    )
}
