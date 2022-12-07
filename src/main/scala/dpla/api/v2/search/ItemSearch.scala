package dpla.api.v2.search
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.api.v2.search.SearchProtocol.{IntermediateSearchResult, SearchCommand}
import dpla.api.v2.search.mappings.DPLAMAPMapper
import dpla.api.v2.search.paramValidators.ItemParamValidator
import dpla.api.v2.search.queryBuilders.DPLAMAPQueryBuilder

/**
 * Handles control flow for conducting item searches and fetches.
 * Public interface for the package.
 */
object ItemSearch extends SearchBehavior {

  override def spawnMapper(
                            context: ActorContext[SearchCommand]
                          ): ActorRef[IntermediateSearchResult] =
    context.spawn(DPLAMAPMapper(), "ItemMapper")

  override def spawnElasticSearchClient(
                                         context: ActorContext[SearchCommand],
                                         mapper: ActorRef[IntermediateSearchResult]
                                       ): ActorRef[IntermediateSearchResult] = {

    val endpoint: String = context.system.settings.config
      .getString("elasticSearch.itemsUrl")
      .stripSuffix("/")

    context.spawn(
      ElasticSearchClient(endpoint, mapper), "ItemElasticSearchClient"
    )
  }

  override def spawnQueryBuilder(
                                  context: ActorContext[SearchCommand],
                                  elasticSearchClient: ActorRef[IntermediateSearchResult]
                                ): ActorRef[IntermediateSearchResult] =
    context.spawn(
      DPLAMAPQueryBuilder(elasticSearchClient), "ItemQueryBuilder"
    )

  override def spawnSearchParamValidator(
                                          context: ActorContext[SearchCommand],
                                          queryBuilder: ActorRef[IntermediateSearchResult]
                                        ): ActorRef[IntermediateSearchResult] =
    context.spawn(
      ItemParamValidator(queryBuilder), "ItemParamValidator"
    )
}
