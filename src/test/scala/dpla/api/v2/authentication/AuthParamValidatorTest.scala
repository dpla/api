package dpla.api.v2.authentication

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import dpla.api.helpers.Utils.fakeApiKey
import dpla.api.v2.authentication.AuthProtocol.{AuthenticationResponse, IntermediateAuthResult, InvalidApiKey, InvalidEmail, RawApiKey, RawEmail, ValidApiKey, ValidEmail}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.Random


class AuthParamValidatorTest extends AnyWordSpec with Matchers
  with BeforeAndAfterAll {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit()

  val interProbe: TestProbe[IntermediateAuthResult] =
    testKit.createTestProbe[IntermediateAuthResult]

  val replyProbe: TestProbe[AuthenticationResponse] =
    testKit.createTestProbe[AuthenticationResponse]

  val paramValidator: ActorRef[IntermediateAuthResult] =
    testKit.spawn(AuthParamValidator(interProbe.ref))

  "email validator" should {

    "accept valid email" in {
      val given = "Email-123@example.com"
      paramValidator ! RawEmail(given, replyProbe.ref)
      interProbe.expectMessageType[ValidEmail]
    }

    "accept special characters in username" in {
      val given = "Email&123@example.com"
      paramValidator ! RawEmail(given, replyProbe.ref)
      interProbe.expectMessageType[ValidEmail]
    }

      "accept plus sign in username (for gmail)" in {
      val given = "Email+123@example.com"
      paramValidator ! RawEmail(given, replyProbe.ref)
      interProbe.expectMessageType[ValidEmail]
    }

      "accept dash in domain name" in {
      val given = "Email-123@ex-ample.com"
      paramValidator ! RawEmail(given, replyProbe.ref)
      interProbe.expectMessageType[ValidEmail]
    }

      "reject too-long email" in {
      val given = Random.alphanumeric.take(100).mkString + "@example.com"
      paramValidator ! RawEmail(given, replyProbe.ref)
      replyProbe.expectMessage(InvalidEmail)
    }
  }

  "api key validator" should {

    "accept valid api key" in {
      val given = fakeApiKey
      paramValidator ! RawApiKey(given, replyProbe.ref)
      interProbe.expectMessageType[ValidApiKey]
    }

    "reject api key with invalid length" in {
      val given = "123"
      paramValidator ! RawApiKey(given, replyProbe.ref)
      replyProbe.expectMessage(InvalidApiKey)
    }

    "reject api key with special characters" in {
      val given = "08e3918eeb8bf446.924f062072459a8"
      paramValidator ! RawApiKey(given, replyProbe.ref)
      replyProbe.expectMessage(InvalidApiKey)
    }
  }
}
