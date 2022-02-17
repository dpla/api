package dpla.ebookapi.v1

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.PostgresProfile.backend.Database

import java.time.LocalDateTime
import scala.concurrent.Future
import scala.util.{Failure, Success}

sealed trait PostgresClientResponse

final case class AccountFound(
                         account: UserAccount
                       ) extends PostgresClientResponse

final case class AccountCreated(
                           apiKey: UserAccount
                         ) extends PostgresClientResponse

case object AccountNotFound extends PostgresClientResponse
case object PostgresError extends PostgresClientResponse

case class UserAccount(
                        apiKey: String,
                        email: String,
                        staff: Boolean,
                        enabled: Boolean
                      )

object PostgresClient {

  sealed trait PostgresClientCommand

  final case class FindAccountByKey(
                               apiKey: String,
                               replyTo: ActorRef[PostgresClientResponse]
                             ) extends PostgresClientCommand

  final case class FindAccountByEmail(
                                 email: String,
                                 replyTo: ActorRef[PostgresClientResponse]
                               ) extends PostgresClientCommand

  final case class CreateAccount(
                            email: String,
                            replyTo: ActorRef[PostgresClientResponse]
                          ) extends PostgresClientCommand

  private final case class ProcessFindResponse(
                                          matches: Seq[(String, String,
                                            Option[Boolean], Option[Boolean])],
                                          replyTo: ActorRef[PostgresClientResponse]
                                        ) extends PostgresClientCommand

  private final case class ReturnFinalResponse(
                                          response: PostgresClientResponse,
                                          replyTo: ActorRef[PostgresClientResponse]
                                        ) extends PostgresClientCommand

  def apply(): Behavior[PostgresClientCommand] = {

    Behaviors.setup { context =>

      val db: Database = Database.forConfig("postgres")

      Behaviors.receiveMessage[PostgresClientCommand] {

        case FindAccountByKey(apiKey, replyTo) =>
          // Find all accounts with the given API key
          val accounts = TableQuery[Account]
          val query = accounts.filter(_.key === apiKey)
            .map(account =>
              (account.key, account.email, account.staff, account.enabled)
            )
          val result: Future[Seq[(String, String, Option[Boolean], Option[Boolean])]] =
            db.run(query.result)

          // Map the Future value to a message, handled by this actor.
          context.pipeToSelf(result) {
            case Success(matches) =>
              ProcessFindResponse(matches, replyTo)
            case Failure(e) =>
              context.log.error("Postgres error:", e)
              ReturnFinalResponse(PostgresError, replyTo)
          }

          Behaviors.same

        case FindAccountByEmail(email, replyTo) =>
          // Find all accounts with the given email
          val accounts = TableQuery[Account]
          val query = accounts.filter(_.email === email)
            .map(account =>
              (account.key, account.email, account.staff, account.enabled)
            )
          val result: Future[Seq[(String, String, Option[Boolean], Option[Boolean])]] =
            db.run(query.result)

          // Map the Future value to a message, handled by this actor.
          context.pipeToSelf(result) {
            case Success(matches) =>
              ProcessFindResponse(matches, replyTo)
            case Failure(e) =>
              context.log.error("Postgres error:", e)
              ReturnFinalResponse(PostgresError, replyTo)
          }

          Behaviors.same

        case CreateAccount(email, replyTo) =>
          // Create an API key for the given email address

          Behaviors.same

        case ProcessFindResponse(matches, replyTo) =>
          matches.headOption match {
            case Some(account) =>
              val key = account._1
              val email = account._2
              val staff = account._3.getOrElse(false)
              val enabled = account._4.getOrElse(true)
              // TODO delete log, for testing only
              context.log.info(s"Found $key $email $staff $enabled")
              replyTo ! AccountFound(UserAccount(key, email, staff, enabled))
            case None =>
              replyTo ! AccountNotFound
          }
          Behaviors.same

        case ReturnFinalResponse(response, replyTo) =>
          replyTo ! response
          Behaviors.same
      }
    }
  }
}

class Account(tag: Tag) extends Table[(Int, String, String, Option[Boolean],
  Option[Boolean], Option[LocalDateTime], Option[LocalDateTime])](tag, "account") {

  def id = column[Int]("id", O.PrimaryKey)
  def key = column[String]("key")
  def email = column[String]("email")
  def enabled = column[Option[Boolean]]("enabled")
  def staff = column[Option[Boolean]]("staff")
  def createdAt = column[Option[LocalDateTime]]("created_at")
  def updatedAt = column[Option[LocalDateTime]]("updated_at")
  def * = (id, key, email, enabled, staff, createdAt, updatedAt)
}
