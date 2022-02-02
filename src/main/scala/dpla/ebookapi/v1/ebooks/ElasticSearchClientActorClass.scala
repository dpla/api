package dpla.ebookapi.v1.ebooks

import akka.actor.Actor
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse}
import akka.pattern.pipe

import scala.concurrent.{ExecutionContext, Future}

class ElasticSearchClientActorClass extends Actor with ElasticSearchQueryBuilder {
  import ElasticSearchClientActor._
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "SingleRequest")

  // TODO can this be a class param?
  val elasticSearchEndpoint: String = System.getenv("ELASTICSEARCH_URL") match {
    case "" => "http://localhost:9200/eleanor"
    case x => x
  }

  implicit val ec: ExecutionContext = context.dispatcher

  def receive: Receive = {
    case EsSearchQuery(params) => search(params).pipeTo(sender())
    case EsFetchQuery(id) => fetch(id).pipeTo(sender())
  }

  private def search(params: SearchParams): Future[HttpResponse] = {
    val uri: String = s"$elasticSearchEndpoint/_search"
    val data: String = composeQuery(params).toString

    val request: HttpRequest = HttpRequest(
      method = HttpMethods.GET,
      uri = uri,
      entity = HttpEntity(ContentTypes.`application/json`, data)
    )

    Http().singleRequest(request)
  }

  def fetch(id: String): Future[HttpResponse] = {
    val uri = s"$elasticSearchEndpoint/_doc/$id"
    Http().singleRequest(HttpRequest(uri = uri))
  }
}

object ElasticSearchClientActorClass {
  sealed trait EsQuery
  final case class EsSearchQuery(params: SearchParams) extends EsQuery
  final case class EsFetchQuery(id: String) extends EsQuery
}
