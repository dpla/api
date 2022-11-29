package dpla.api.v2.search
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.api.v2.search.SearchProtocol.{IntermediateSearchResult, SearchCommand}
import dpla.api.v2.search.paramValidators.PssParamValidator

/**
 * Handles control flow for conducting primary source set searches and fetches.
 * Public interface for the package.
 */
object PssSearch extends SearchBehavior {

  override def spawnMapper(
                            context: ActorContext[SearchCommand]
                          ): ActorRef[IntermediateSearchResult] = {
    // TODO change mapper
    context.spawn(DPLAMAPMapper(), "PssMapper")
  }

  override def spawnElasticSearchClient(
                                         context: ActorContext[SearchCommand],
                                         mapper: ActorRef[IntermediateSearchResult]
                                       ): ActorRef[IntermediateSearchResult] = {

    val endpoint: String = context.system.settings.config
      .getString("elasticSearch.pssUrl")
      .stripSuffix("/")

    context.spawn(
      ElasticSearchClient(endpoint, mapper), "PssElasticSearchClient"
    )
  }

  override def spawnQueryBuilder(
                                  context: ActorContext[SearchCommand],
                                  elasticSearchClient: ActorRef[IntermediateSearchResult]
                                ): ActorRef[IntermediateSearchResult] =
    context.spawn(
      QueryBuilder(elasticSearchClient), "PssQueryBuilder"
    )

  override def spawnSearchParamValidator(
                                          context: ActorContext[SearchCommand],
                                          queryBuilder: ActorRef[IntermediateSearchResult]
                                        ): ActorRef[IntermediateSearchResult] =
    context.spawn(
      PssParamValidator(queryBuilder), "PssParamValidator"
    )
}
