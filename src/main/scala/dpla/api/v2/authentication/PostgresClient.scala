package dpla.api.v2.authentication

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import dpla.api.v2.authentication.AuthProtocol.{AccountCreated, AccountFound, AccountNotFound, AuthenticationFailure, AuthenticationResponse, IntermediateAuthResult, ValidApiKey, ValidEmail}
import org.apache.commons.codec.digest.DigestUtils
import slick.jdbc.GetResult
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.PostgresProfile.backend.Database

import scala.concurrent.Future
import scala.util.{Failure, Random, Success}


/**
 * Handles interactions with the postgres database.
 */

/** Case class for modeling a user account */
case class Account(
                    id: Int,
                    key: String,
                    email: String,
                    enabled: Option[Boolean],
                    staff: Option[Boolean]
                  )

object PostgresClient {

  private sealed trait PostgresClientCommand extends IntermediateAuthResult


  private final case class FindUserByEmail(
                                            email: String,
                                            replyTo: ActorRef[AuthenticationResponse]
                                          ) extends PostgresClientCommand

  private final case class ProcessFindResponse(
                                                matches: Seq[Account],
                                                replyTo: ActorRef[AuthenticationResponse]
                                              ) extends PostgresClientCommand

  private final case class ReturnFinalResponse(
                                                response: AuthenticationResponse,
                                                replyTo: ActorRef[AuthenticationResponse],
                                                error: Option[Throwable] = None
                                              ) extends PostgresClientCommand

  def apply(): Behavior[IntermediateAuthResult] = {

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

      Behaviors.receiveMessage[IntermediateAuthResult] {

        case ValidEmail(email, replyTo) =>
          val newKey: String =
            DigestUtils.md5Hex(email + Random.alphanumeric.take(32).mkString)
          val staff: Boolean = if (email.endsWith(".dp.la")) true else false
          val enabled: Boolean = true

          // For mapping results of plain SQL query into Account case class
          implicit val getAccountResult: AnyRef with GetResult[Account] =
            GetResult(a => Account(a.<<, a.<<, a.<<, a.<<, a.<<))

          // Create an API key for the given email address
          // unless an account already exists for the email address
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
                  FindUserByEmail(email, replyTo)
              }
            case Failure(e) =>
              context.log.error("Postgres error:", e)
              ReturnFinalResponse(AuthenticationFailure, replyTo)
          }
          Behaviors.same

        case ValidApiKey(apiKey, replyTo) =>
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
              ReturnFinalResponse(AuthenticationFailure, replyTo, Some(e))
          }
          Behaviors.same

        case FindUserByEmail(email, replyTo) =>
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
              ReturnFinalResponse(AuthenticationFailure, replyTo)
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

        case ReturnFinalResponse(response, replyTo, error) =>
          // Log error if one exists
          error match {
            case Some(e) =>
              context.log.error(
                "Failed to process a Future", e
              )
            case None => // no-op
          }
          replyTo ! response
          Behaviors.same

        case _ =>
          Behaviors.unhandled
      }
    }
  }
}
