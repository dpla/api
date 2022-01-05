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

  def all: Future[Either[StatusCode, Future[Publications]]] = {
    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "SingleRequest")
    // needed for the future map/onComplete
    implicit val executionContext: ExecutionContextExecutor = system.executionContext

    val uri = s"$elasticSearchEndpoint/_search?q=*"

    System.out.println(uri)

    val response: Future[HttpResponse] =
      Http().singleRequest(HttpRequest(uri = uri))

    response.map(res => {
      res.status.intValue match {
        case 200 =>
          val body: Future[String] = Unmarshaller.stringUnmarshaller(res.entity)
          val pubs: Future[Publications] = body.map(_.parseJson.convertTo[Publications])
          Right(pubs)
        case _ =>
          Left(res.status)
      }
    })
  }

  def find(id: String): Future[Either[StatusCode, Future[Publication]]] = {
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
          val pub: Future[Publication] = body.map(_.parseJson.convertTo[Publication])
          Right(pub)
        case _ => Left(res.status)
      }
    })
  }
}
