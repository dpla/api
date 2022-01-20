package dpla.ebookapi.v1.ebooks

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling._
import dpla.ebookapi.v1.ebooks.JsonFormats._
import spray.json._

import scala.concurrent.{ExecutionContextExecutor, Future}

class ElasticSearchClient(elasticSearchEndpoint: String) {

  def fetch(id: String): Future[Either[StatusCode, Future[SinglePublication]]] = {
    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "SingleRequest")
    // needed for the future map
    implicit val executionContext: ExecutionContextExecutor = system.executionContext

    val uri = s"$elasticSearchEndpoint/_doc/$id"

    val response: Future[HttpResponse] =
      Http().singleRequest(HttpRequest(uri = uri))

    response.map(res => {
      res.status.intValue match {
        case 200 =>
          val body: Future[String] = Unmarshaller.stringUnmarshaller(res.entity)
          val pub: Future[SinglePublication] = body.map(_.parseJson.convertTo[SinglePublication])
          Right(pub)
        case _ => Left(res.status)
      }
    })
  }

  def search(params: SearchParams): Future[Either[StatusCode, Future[PublicationList]]] = {
    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "SingleRequest")
    // needed for the future map
    implicit val executionContext: ExecutionContextExecutor = system.executionContext

    val uri: String = s"$elasticSearchEndpoint/_search"
    val data: String = ElasticSearchQueryBuilder.composeQuery(params).toString

    val request: HttpRequest = HttpRequest(
      method = HttpMethods.GET,
      uri = uri,
      entity = HttpEntity(ContentTypes.`application/json`, data)
    )

    val response: Future[HttpResponse] = Http().singleRequest(request)

    response.map(res => {
      res.status.intValue match {
        case 200 =>
          val body: Future[String] = Unmarshaller.stringUnmarshaller(res.entity)
          val pubs: Future[PublicationList] =
            body.map(_.parseJson.convertTo[PublicationList].copy(limit=Some(params.pageSize), start=Some(params.start)))
          Right(pubs)
        case _ =>
          Left(res.status)
      }
    })
  }
}
