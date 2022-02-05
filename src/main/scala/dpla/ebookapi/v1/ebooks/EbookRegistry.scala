package dpla.ebookapi.v1.ebooks

import akka.NotUsed
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.ebookapi.v1.ebooks.EbookMapper.{MapFetchResponse, MapSearchResponse}
import dpla.ebookapi.v1.ebooks.ElasticSearchClient.{EsClientCommand, GetEsFetchResult, GetEsSearchResult}
import dpla.ebookapi.v1.ebooks.ElasticSearchResponseProcessor.ProcessElasticSearchResponse
import dpla.ebookapi.v1.ebooks.ParamValidator.{ValidateFetchParams, ValidateSearchParams}


sealed trait RegistryResponse
final case class SearchResult(result: EbookList) extends RegistryResponse
final case class FetchResult(result: SingleEbook) extends RegistryResponse
case class ValidationFailure(message: String) extends RegistryResponse
case object NotFoundFailure extends RegistryResponse
case object InternalFailure extends RegistryResponse

object EbookRegistry {

  sealed trait RegistryCommand

  final case class Search(
                           client: ActorRef[EsClientCommand],
                           rawParams: Map[String, String],
                           replyTo: ActorRef[RegistryResponse]
                         ) extends RegistryCommand

  final case class Fetch(
                          client: ActorRef[EsClientCommand],
                          id: String,
                          rawParams: Map[String, String],
                          replyTo: ActorRef[RegistryResponse]
                        ) extends RegistryCommand

  def apply(): Behavior[RegistryCommand] = {
    Behaviors.setup[RegistryCommand] { context =>
      val paramValidator: ActorRef[ParamValidator.ValidationRequest] =
        context.spawn(ParamValidator(), "ParamValidator")
      val mapper: ActorRef[EbookMapper.MapperCommand] =
        context.spawn(EbookMapper(), "EbookMapper")

      Behaviors.receiveMessage[RegistryCommand] {

        case Search(client, rawParams, replyTo) =>
          // Create a session child actor to process the request
          val sessionChildActor =
            processSearch(rawParams, replyTo, paramValidator, client, mapper)
          val uniqueId = java.util.UUID.randomUUID.toString
          context.spawn(sessionChildActor, s"ProcessSearchRequest-$uniqueId")
          Behaviors.same

        case Fetch(client, id, rawParams, replyTo) =>
          // Create a session child actor to process the request
          val sessionChildActor =
            processFetch(id, rawParams, replyTo, paramValidator, client, mapper)
          val uniqueId = java.util.UUID.randomUUID.toString
          context.spawn(sessionChildActor, s"ProcessFetchRequest-$uniqueId")
          Behaviors.same
      }
    }
  }

  /**
   * Per session actor behavior for handling a search request.
   * The session actor has its own internal state and its own ActorRef for sending/receiving messages.
   */
  private def processSearch(
                     rawParams: Map[String, String],
                     replyTo: ActorRef[RegistryResponse],
                     paramValidator: ActorRef[ParamValidator.ValidationRequest],
                     client: ActorRef[ElasticSearchClient.EsClientCommand],
                     mapper: ActorRef[EbookMapper.MapperCommand],
                   ): Behavior[NotUsed] = {

    Behaviors.setup[AnyRef] { context =>

      var searchParams: Option[SearchParams] = None

      paramValidator ! ValidateSearchParams(rawParams, context.self)

      Behaviors.receiveMessage {
        case ValidSearchParams(params: SearchParams) =>
          searchParams = Some(params)
          client ! GetEsSearchResult(params, context.self)
          Behaviors.same

        case ValidationError(message) =>
          replyTo ! ValidationFailure(message)
          Behaviors.stopped

        case ElasticSearchSuccess(body) =>
          searchParams match {
            case Some(params) =>
              mapper ! MapSearchResponse(body, params.page, params.pageSize, context.self)
              Behaviors.same
            case None =>
              // TODO log error
              replyTo ! InternalFailure
              Behaviors.stopped
          }

        case ElasticSearchHttpFailure =>
          replyTo ! InternalFailure
          Behaviors.stopped

        case ElasticSearchUnreachable =>
          replyTo ! InternalFailure
          Behaviors.stopped

        case MappedEbookList(ebookList) =>
          replyTo ! SearchResult(ebookList)
          Behaviors.stopped

        case MapFailure =>
          replyTo ! InternalFailure
          Behaviors.stopped

        case _ =>
          // TODO log?
          Behaviors.unhandled
      }
    }.narrow[NotUsed]
  }

  /**
   * Per session actor behavior for handling a fetch request.
   * The session actor has its own internal state and its own ActorRef for sending/receiving messages.
   */
  private def processFetch(
                     id: String,
                     rawParams: Map[String, String],
                     replyTo: ActorRef[RegistryResponse],
                     paramValidator: ActorRef[ParamValidator.ValidationRequest],
                     client: ActorRef[ElasticSearchClient.EsClientCommand],
                     mapper: ActorRef[EbookMapper.MapperCommand],
                   ): Behavior[NotUsed] = {

    Behaviors.setup[AnyRef] { context =>

      paramValidator ! ValidateFetchParams(id, rawParams, context.self)

      Behaviors.receiveMessage {
        case ValidFetchParams(params: FetchParams) =>
          client ! GetEsFetchResult(params, context.self)
          Behaviors.same

        case ValidationError(message) =>
          replyTo ! ValidationFailure(message)
          Behaviors.stopped

        case ElasticSearchSuccess(body) =>
          mapper ! MapFetchResponse(body, context.self)
          Behaviors.same

        case ElasticSearchHttpFailure(statusCode) =>
          if (statusCode.intValue == 404)
            replyTo ! NotFoundFailure
          else
            replyTo ! InternalFailure
          Behaviors.stopped

        case ElasticSearchUnreachable =>
          replyTo ! InternalFailure
          Behaviors.stopped

        case MappedSingleEbook(singleEbook) =>
          replyTo ! FetchResult(singleEbook)
          Behaviors.stopped

        case MapFailure =>
          replyTo ! InternalFailure
          Behaviors.stopped

        case _ =>
          // TODO log?
          Behaviors.unhandled
      }
    }.narrow[NotUsed]
  }
}
