package dpla.api.v2.smr

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import dpla.api.v2.registry.SmrArchiveRequest
import dpla.api.v2.smr.SmrProtocol._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec


class SmrParamValidatorTest extends AnyWordSpec with Matchers
  with BeforeAndAfterAll {

  lazy val testKit: ActorTestKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  val replyProbe: TestProbe[SmrResponse] =
    testKit.createTestProbe[SmrResponse]

  val interProbe: TestProbe[IntermediateSmrResult] =
    testKit.createTestProbe[IntermediateSmrResult]

  val smrParamValidator: ActorRef[IntermediateSmrResult] =
    testKit.spawn(SmrParamValidator(interProbe.ref))

  val defaultService = "tiktok"
  val defaultPost = "123"
  val defaultUser = "abc"

  val defaultRequest: SmrArchiveRequest = SmrArchiveRequest(
    service = Some(defaultService),
    post = Some(defaultPost),
    user = Some(defaultUser)
  )

  "smr param validator" should {
    "require service" in {
      val request = defaultRequest.copy(service = None)
      smrParamValidator ! RawSmrParams(request, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSmrParams]
    }

    "require post" in {
      val request = defaultRequest.copy(post = None)
      smrParamValidator ! RawSmrParams(request, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSmrParams]
    }

    "require user" in {
      val request = defaultRequest.copy(user = None)
      smrParamValidator ! RawSmrParams(request, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSmrParams]
    }

    "accept valid service" in {
      smrParamValidator ! RawSmrParams(defaultRequest, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSmrParams]
      msg.params.service should be(defaultService)
    }

    "reject invalid service" in {
      val request = defaultRequest.copy(service = Some("foo"))
      smrParamValidator ! RawSmrParams(request, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSmrParams]
    }

    "accept valid post" in {
      smrParamValidator ! RawSmrParams(defaultRequest, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSmrParams]
      msg.params.post should be(defaultPost)
    }

    "reject invalid post" in {
      val request = defaultRequest.copy(post = Some("<blah>"))
      smrParamValidator ! RawSmrParams(request, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSmrParams]
    }

    "accept valid user" in {
      smrParamValidator ! RawSmrParams(defaultRequest, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSmrParams]
      msg.params.user should be(defaultUser)
    }

    "reject invalid user" in {
      val request = defaultRequest.copy(user = Some("bad()=user"))
      smrParamValidator ! RawSmrParams(request, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSmrParams]
    }
  }
}