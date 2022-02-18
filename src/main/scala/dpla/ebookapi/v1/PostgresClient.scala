package dpla.ebookapi.v1

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import slick.jdbc.GetResult
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.PostgresProfile.api.actionBasedSQLInterpolation
import slick.jdbc.PostgresProfile.backend.Database

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.util.Random


sealed trait PostgresClientResponse

final case class AccountFound(
                         account: Account
                       ) extends PostgresClientResponse

final case class AccountCreated(
                           account: Account
                         ) extends PostgresClientResponse

case object AccountNotFound extends PostgresClientResponse
case object PostgresError extends PostgresClientResponse

case class Account(
                    id: Int,
                    key: String,
                    email: String,
                    enabled: Option[Boolean],
                    staff: Option[Boolean]
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
                                          matches: Seq[Account],
                                          replyTo: ActorRef[PostgresClientResponse]
                                        ) extends PostgresClientCommand

  private final case class ReturnFinalResponse(
                                          response: PostgresClientResponse,
                                          replyTo: ActorRef[PostgresClientResponse]
                                        ) extends PostgresClientCommand

  def apply(): Behavior[PostgresClientCommand] = {

    Behaviors.setup { context =>

      val db: Database = Database.forConfig("postgres")

      // Models 'account' table in database.
      // Does not include created_at and updated_at columns b/c they are not
      // needed, and modeling SQL dates in Java can be tricky.
      // Query results are mapped to Account case class.
      class Accounts(tag: Tag) extends Table[Account](tag, "account") {
        def id = column[Int]("id", O.PrimaryKey)
        def key = column[String]("key")
        def email = column[String]("email")
        def enabled = column[Option[Boolean]]("enabled")
        def staff = column[Option[Boolean]]("staff")
        def * = (id, key, email, enabled, staff) <> (Account.tupled, Account.unapply)
      }

      Behaviors.receiveMessage[PostgresClientCommand] {

        case CreateAccount(email, replyTo) =>
          // Forcing lowercase on the API key makes it backward-compatible with
          // the old DPLA API application-level validations.
          // TODO use md5 to generate new key
          val newKey: String = Random.alphanumeric.take(32).mkString.toLowerCase
          val staff: Boolean = if (email.endsWith(".dp.la")) true else false
          val enabled: Boolean = true

          // For mapping results of plain SQL query into Account case class
          implicit val getAccountResult: AnyRef with GetResult[Account] =
            GetResult(a => Account(a.<<, a.<<, a.<<, a.<<, a.<<))

          // Create an API key for the given email address
          // unless an account already exists for the email address
          // TODO also update (upsert?) if an account has been disabled
          val insertAction =
            sql"""INSERT INTO account (key, email, enabled, staff)
                SELECT $newKey, $email, $enabled, $staff
                WHERE NOT EXISTS (SELECT id FROM account WHERE email = $email)
                RETURNING id, key, email, enabled, staff;"""
            .as[Account]

          val result: Future[Seq[Account]] = db.run(insertAction)

          // Map the future value to a message, handled by this actor.
          context.pipeToSelf(result) {
            case Success(created) =>
              created.headOption match {
                case Some(account) =>
                  ReturnFinalResponse(AccountCreated(account), replyTo)
                case None =>
                  FindAccountByEmail(email, replyTo)
              }
            case Failure(e) =>
              context.log.error("Postgres error:", e)
              ReturnFinalResponse(PostgresError, replyTo)
          }

          Behaviors.same


        case FindAccountByKey(apiKey, replyTo) =>
          // Find all accounts with the given API key
          val accounts = TableQuery[Accounts]
          val query = accounts.filter(_.key === apiKey)
          val result: Future[Seq[Account]] = db.run(query.result)

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
          val result: Future[Seq[Account]] = db.run(query.result)

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
              replyTo ! AccountFound(account)
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
