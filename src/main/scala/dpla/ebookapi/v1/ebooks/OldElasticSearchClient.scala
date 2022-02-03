package dpla.ebookapi.v1.ebooks

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling._
import dpla.ebookapi.v1.ebooks.JsonFormats._
import spray.json._

import scala.concurrent.{ExecutionContextExecutor, Future}

class OldElasticSearchClient(elasticSearchEndpoint: String) {

  def fetch(id: String): Future[Either[StatusCode, Future[SingleEbook]]] = {
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
          val ebook: Future[SingleEbook] = body.map(_.parseJson.convertTo[SingleEbook])
          Right(ebook)
        case _ =>
          Left(res.status)
      }
    })
  }

  def search(params: SearchParams): Future[Either[StatusCode, Future[EbookList]]] = {
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
          val ebooks: Future[EbookList] =
            body.map(_.parseJson.convertTo[EbookList].copy(limit=Some(params.pageSize), start=Some(params.start)))
          Right(ebooks)
        case _ =>
          Left(res.status)
      }
    })
  }
}
