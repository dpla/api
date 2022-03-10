package dpla.ebookapi.v1.search

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.ebookapi.v1.search.SearchProtocol.{IntermediateSearchResult, SearchCommand}

class MockEbookSearch(testKit: ActorTestKit) {

  private var elasticSearchClient: Option[ActorRef[IntermediateSearchResult]] = None
  private var mapper: Option[ActorRef[IntermediateSearchResult]] = None

  def setElasticSearchClient(ref: ActorRef[IntermediateSearchResult]): Unit =
    elasticSearchClient = Some(ref)

  def setEbookMapper(ref: ActorRef[IntermediateSearchResult]): Unit =
    mapper = Some(ref)

  object Mock extends SearchBehavior {

    override def spawnMapper(
                              context: ActorContext[SearchCommand]
                            ): ActorRef[IntermediateSearchResult] = {
      mapper.getOrElse(
        context.spawnAnonymous(EbookMapper())
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

    override def spawnSearchParamValidator(
                                            context: ActorContext[SearchCommand],
                                            queryBuilder: ActorRef[IntermediateSearchResult],
                                            elasticSearchClient: ActorRef[IntermediateSearchResult]
                                          ): ActorRef[IntermediateSearchResult] =
      context.spawn(
        EbookParamValidator(queryBuilder, elasticSearchClient), "EbookParamValidator"
      )
  }

  def getRef: ActorRef[SearchCommand] = testKit.spawn(Mock())
}
