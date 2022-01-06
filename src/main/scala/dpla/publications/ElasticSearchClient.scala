package dpla.publications

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import scala.concurrent.{ExecutionContextExecutor, Future}
import spray.json._
import JsonFormats._
import akka.http.scaladsl.unmarshalling._

class ElasticSearchClient(elasticSearchEndpoint: String) {

  def search(): Future[Either[StatusCode, Future[PublicationList]]] = {
    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "SingleRequest")
    // needed for the future map/onComplete
    implicit val executionContext: ExecutionContextExecutor = system.executionContext

    val uri = s"$elasticSearchEndpoint/_search"

    System.out.println(uri)

    val data: String =
      """
        |{
        |  "query" : {
        |     "match_all": {}
        |  }
        |}
        |""".stripMargin

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
          val pubs: Future[PublicationList] = body.map(_.parseJson.convertTo[PublicationList])
          Right(pubs)
        case _ =>
          Left(res.status)
      }
    })
  }

  def fetch(id: String): Future[Either[StatusCode, Future[SinglePublication]]] = {
    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "SingleRequest")
    // needed for the future map/onComplete
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
}
