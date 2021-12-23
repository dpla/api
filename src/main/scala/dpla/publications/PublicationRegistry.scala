package dpla.publications

//#publication-registry-actor
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._

import spray.json._
import JsonFormats._

import scala.collection.immutable
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

//final case class Publication(id: String, title: String)
//final case class Publications(publications: immutable.Seq[Publication])

object PublicationRegistry {
  // actor protocol
  sealed trait Command
//  final case class GetPublications(replyTo: ActorRef[Publications]) extends Command
  final case class GetPublications(replyTo: ActorRef[GetPublicationsResponse]) extends Command
  final case class GetPublication(id: String, replyTo: ActorRef[GetPublicationResponse]) extends Command
  final case class GetPublicationsResponse(maybePublications: Future[Either[StatusCode, Future[Publications]]])
  final case class GetPublicationResponse(maybePublication: Option[Publication])
  final case class ActionPerformed(description: String)

  def apply(): Behavior[Command] = registry(Set.empty)

  private def registry(publications: Set[Publication]): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetPublications(replyTo) =>
        replyTo ! GetPublicationsResponse(getAllPublications)
        Behaviors.same
      case GetPublication(id, replyTo) =>
//        replyTo ! GetPublicationResponse(publications.find(_.id == id))
        Behaviors.same
    }

  def getAllPublications: Future[Either[StatusCode, Future[Publications]]] = {
    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "GetAllPublications")
    // needed for the future map/onComplete
    implicit val executionContext: ExecutionContextExecutor = system.executionContext

    ElasticSearchClient.all

//    val foo: Unit = response onComplete {
//      case Failure(e) => Left(ElasticSearchError(500, e.getMessage))
//      case Success(res) => {
//        res.status.intValue match {
//          case 200 => Right(res.entity.toString.parseJson.convertTo[Publications])
//          case _ => Left(ElasticSearchError(res.status.intValue, res.status.reason))
//        }
//      }
//    }


//    response.map(res => {
//      res.status.intValue match {
//        case 200 => Right(res.entity.toString.parseJson.convertTo[Publications])
//        case _ => Left(res.status)
//      }
//    })

  }
}

