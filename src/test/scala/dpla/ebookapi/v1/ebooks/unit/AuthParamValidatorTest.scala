package dpla.ebookapi.v1.ebooks.unit

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import dpla.ebookapi.mocks.MockPostgresClientSuccess
import dpla.ebookapi.v1.authentication.{AuthParamValidator, AuthParamValidatorResponse, InvalidAuthParam, PostgresClientResponse, UserCreated, UserFound}
import dpla.ebookapi.v1.authentication.AuthParamValidator.{AuthParamValidatorCommand, ValidateApiKey, ValidateEmail}
import dpla.ebookapi.v1.authentication.PostgresClient.PostgresClientCommand
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.Random


class AuthParamValidatorTest extends AnyWordSpec with Matchers
  with BeforeAndAfterAll {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit()

  val baseParams = Map("api_key" -> "08e3918eeb8bf4469924f062072459a8")

  val postgresClient: ActorRef[PostgresClientCommand] =
    testKit.spawn(MockPostgresClientSuccess())

  val paramValidator: ActorRef[AuthParamValidatorCommand] =
    testKit.spawn(AuthParamValidator(postgresClient))
  val validationProbe: TestProbe[AuthParamValidatorResponse] =
    testKit.createTestProbe[AuthParamValidatorResponse]
  val postgresProbe: TestProbe[PostgresClientResponse] =
    testKit.createTestProbe[PostgresClientResponse]

  "email validator" should {

    "accept valid email" in {
      val given = "Email-123@example.com"
      paramValidator ! ValidateEmail(given, postgresProbe.ref, validationProbe.ref)
      postgresProbe.expectMessageType[UserCreated]
    }

    "accept special characters in username" in {
      val given = "Email&123@example.com"
      paramValidator ! ValidateEmail(given, postgresProbe.ref, validationProbe.ref)
      postgresProbe.expectMessageType[UserCreated]
    }

      "accept plus sign in username (for gmail)" in {
      val given = "Email+123@example.com"
      paramValidator ! ValidateEmail(given, postgresProbe.ref, validationProbe.ref)
      postgresProbe.expectMessageType[UserCreated]
    }

      "accept dash in domain name" in {
      val given = "Email-123@ex-ample.com"
      paramValidator ! ValidateEmail(given, postgresProbe.ref, validationProbe.ref)
      postgresProbe.expectMessageType[UserCreated]
    }

      "reject too-long email" in {
      val given = Random.alphanumeric.take(100).mkString + "@example.com"
      paramValidator ! ValidateEmail(given, postgresProbe.ref, validationProbe.ref)
      validationProbe.expectMessage(InvalidAuthParam)
    }
  }

  "api key validator" should {

    "accept valid api key" in {
      val given = "08e3918eeb8bf4469924f062072459a8"
      paramValidator ! ValidateApiKey(given, postgresProbe.ref, validationProbe.ref)
      postgresProbe.expectMessageType[UserFound]
    }

    "reject api key with invalid length" in {
      val given = "123"
      paramValidator ! ValidateApiKey(given, postgresProbe.ref, validationProbe.ref)
      validationProbe.expectMessage(InvalidAuthParam)
    }

    "reject api key with special characters" in {
      val given = "08e3918eeb8bf446.924f062072459a8"
      paramValidator ! ValidateApiKey(given, postgresProbe.ref, validationProbe.ref)
      validationProbe.expectMessage(InvalidAuthParam)
    }
  }
}
