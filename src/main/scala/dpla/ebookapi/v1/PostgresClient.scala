package dpla.ebookapi.v1

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, LoggerOps}
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.PostgresProfile.backend.Database
import slick.sql.SqlProfile.ColumnOption.SqlType

import java.time.LocalDateTime
import scala.concurrent.Future
import scala.util.{Failure, Success}

sealed trait PostgresClientResponse

case class ApiKeyFound(
                        apiKey: ApiKey
                      ) extends PostgresClientResponse

case class ApiKeyCreated(
                          apiKey: ApiKey
                        ) extends PostgresClientResponse

object ApiKeyNotFound extends PostgresClientResponse
object PostgresError extends PostgresClientResponse

case class ApiKey(
                   key: String,
                   email: String,
                   staff: Boolean,
                   enabled: Boolean
                 )

object PostgresClient {

  sealed trait PostgresClientCommand

  case class FindApiKey(
                         apiKey: String,
                         replyTo: ActorRef[PostgresClientResponse]
                       ) extends PostgresClientCommand

  case class CreateApiKey(
                           email: String,
                           replyTo: ActorRef[PostgresClientResponse]
                         ) extends PostgresClientCommand

  private case class ProcessFindResponse(
                                          matches: Seq[(String, String,
                                            Option[Boolean], Option[Boolean])],
                                          replyTo: ActorRef[PostgresClientResponse]
                                        ) extends PostgresClientCommand

  private case class ReturnFinalResponse(
                                          response: PostgresClientResponse,
                                          replyTo: ActorRef[PostgresClientResponse]
                                        ) extends PostgresClientCommand

  def apply(): Behavior[PostgresClientCommand] = {

    Behaviors.setup { context =>

      val db: Database = Database.forConfig("postgres")

      Behaviors.receiveMessage[PostgresClientCommand] {

        case FindApiKey(apiKey, replyTo) =>
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
              context.log.error("Failed to reach Postgres: ", e)
              ReturnFinalResponse(PostgresError, replyTo)
          }

          Behaviors.same

        case CreateApiKey(email, replyTo) =>
          // Create an API key for the given email address

          Behaviors.same

        case ProcessFindResponse(matches, replyTo) =>
          matches.headOption match {
            case Some(account) =>
              val key = account._1
              val email = account._2
              val staff = account._3.getOrElse(false)
              val enabled = account._4.getOrElse(true)
              context.log.info(s"Found $email $staff $enabled")
              val internalAccount: Boolean = email.endsWith(".dp.la") || staff
              replyTo ! ApiKeyFound(ApiKey(key, email, staff, enabled))
            case None =>
              replyTo ! ApiKeyNotFound
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
