package dpla.ebookapi.v1.ebooks.unit

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import dpla.ebookapi.v1.apiKey.ApiKeyParamValidator.{ApiKeyValidationCommand, ValidateEmail}
import dpla.ebookapi.v1.apiKey.{ApiKeyParamValidator, ValidEmail}
import dpla.ebookapi.v1.{InvalidParams, ValidationResponse}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.Random


class ApiKeyParamValidatorTest extends AnyWordSpec with Matchers
  with BeforeAndAfterAll {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit()

  val baseParams = Map("api_key" -> "08e3918eeb8bf4469924f062072459a8")

  val paramValidator: ActorRef[ApiKeyValidationCommand] =
    testKit.spawn(ApiKeyParamValidator())
  val probe: TestProbe[ValidationResponse] =
    testKit.createTestProbe[ValidationResponse]

  "email validator" should {

    "accept valid email" in {
      val given = "Email-123@example.com"
      paramValidator ! ValidateEmail(given, probe.ref)
      probe.expectMessageType[ValidEmail]
    }

      "accept special characters in username" in {
      val given = "Email&123@example.com"
      paramValidator ! ValidateEmail(given, probe.ref)
      probe.expectMessageType[ValidEmail]
    }

      "accept plus sign in username (for gmail)" in {
      val given = "Email+123@example.com"
      paramValidator ! ValidateEmail(given, probe.ref)
      probe.expectMessageType[ValidEmail]
    }

      "accept dash in domain name" in {
      val given = "Email-123@ex-ample.com"
      paramValidator ! ValidateEmail(given, probe.ref)
      probe.expectMessageType[ValidEmail]
    }

      "reject too-long email" in {
      val given = Random.alphanumeric.take(100).mkString + "@example.com"
      paramValidator ! ValidateEmail(given, probe.ref)
      probe.expectMessageType[InvalidParams]
    }
  }
}