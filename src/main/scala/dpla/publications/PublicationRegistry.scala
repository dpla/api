package dpla.publications

//#publication-registry-actor
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import scala.collection.immutable

final case class Publication(id: String, title: String)
final case class Publications(publications: immutable.Seq[Publication])

object PublicationRegistry {
  // actor protocol
  sealed trait Command
  final case class GetPublications(replyTo: ActorRef[Publications]) extends Command
  final case class GetPublication(id: String, replyTo: ActorRef[GetPublicationResponse]) extends Command
  final case class GetPublicationResponse(maybePublication: Option[Publication])
  final case class ActionPerformed(description: String)

  def apply(): Behavior[Command] = registry(Set.empty)

  private def registry(publications: Set[Publication]): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetPublications(replyTo) =>
        replyTo ! Publications(publications.toSeq)
        Behaviors.same
      case GetPublication(id, replyTo) =>
        replyTo ! GetPublicationResponse(publications.find(_.id == id))
        Behaviors.same
    }
}
