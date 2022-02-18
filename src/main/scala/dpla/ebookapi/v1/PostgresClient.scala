package dpla.ebookapi.v1

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.PostgresProfile.api.actionBasedSQLInterpolation
import slick.jdbc.PostgresProfile.backend.Database

import java.time.LocalDateTime
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.util.Random


sealed trait PostgresClientResponse

final case class AccountFound(
                         account: UserAccount
                       ) extends PostgresClientResponse

final case class AccountCreated(
                           account: UserAccount
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

        case CreateAccount(email, replyTo) =>
          // Forcing lowercase on the API key makes it backward-compatible with
          // the old DPLA API application-level validations.
          val newKey: String = Random.alphanumeric.take(32).mkString.toLowerCase
          val staff: Boolean = if (email.endsWith(".dp.la")) true else false
          val enabled: Boolean = true

          // Create an API key for the given email address
          // unless an account already exists for the email address
          val insertAction: DBIO[Int] =
            sqlu"INSERT INTO account (key, email, enabled, staff) SELECT $newKey, $email, $enabled, $staff WHERE NOT EXISTS (SELECT id FROM account WHERE email = $email);"

          val result: Future[Int] = db.run(insertAction)

          // Map the future value to a message, handled by this actor.
          context.pipeToSelf(result) {
            case Success(columns) =>
              if (columns == 1) {
                val newAccount = UserAccount(newKey, email, staff, enabled)
                ReturnFinalResponse(AccountCreated(newAccount), replyTo)
              }
              else
                FindAccountByEmail(email, replyTo)

            case Failure(e) =>
              context.log.error("Postgres error:", e)
              ReturnFinalResponse(PostgresError, replyTo)
          }

          Behaviors.same

        case FindAccountByKey(apiKey, replyTo) =>
          // Find all accounts with the given API key
          val accounts = TableQuery[Accounts]
          val query = accounts.filter(_.key === apiKey)
            .map(a => (a.key, a.email, a.staff, a.enabled))
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
          val accounts = TableQuery[Accounts]
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

/**
 * Database models
 */

case class Account(
                    id: Int,
                    key: String,
                    email: String,
                    enabled: Option[Boolean],
                    staff: Option[Boolean],
                    createdAt: Option[LocalDateTime],
                    updatedAt: Option[LocalDateTime]
                  )

class Accounts(tag: Tag) extends Table[Account](tag, "account") {

  def id = column[Int]("id", O.PrimaryKey)
  def key = column[String]("key")
  def email = column[String]("email")
  def enabled = column[Option[Boolean]]("enabled")
  def staff = column[Option[Boolean]]("staff")
  def createdAt = column[Option[LocalDateTime]]("created_at")
  def updatedAt = column[Option[LocalDateTime]]("updated_at")
  def * = (id, key, email, enabled, staff, createdAt, updatedAt) <> (Account.tupled, Account.unapply)
}
