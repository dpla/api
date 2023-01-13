package dpla.api.v2.smr

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
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

  "smr param validator" should {
    "require service" in {
      val params = Map("post" -> defaultPost, "user" -> defaultUser)
      smrParamValidator ! RawSmrParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSmrParams]
    }

    "require post" in {
      val params = Map("service" -> defaultService, "user" -> defaultUser)
      smrParamValidator ! RawSmrParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSmrParams]
    }

    "require user" in {
      val params = Map("service" -> defaultService, "post" -> defaultPost)
      smrParamValidator ! RawSmrParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSmrParams]
    }

    "reject unrecognized params" in {
      val params = Map(
        "service" -> defaultService,
        "post" -> defaultPost,
        "user" -> defaultUser,
        "foo" -> "bar"
      )
      smrParamValidator ! RawSmrParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSmrParams]
    }

    "accept valid service" in {
      val params = Map(
        "service" -> defaultService,
        "post" -> defaultPost,
        "user" -> defaultUser
      )
      smrParamValidator ! RawSmrParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSmrParams]
      msg.params.service should be(defaultService)
    }

    "reject invalid service" in {
      val params = Map(
        "service" -> "foo",
        "post" -> defaultPost,
        "user" -> defaultUser
      )
      smrParamValidator ! RawSmrParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSmrParams]
    }

    "accept valid post" in {
      val params = Map(
        "service" -> defaultService,
        "post" -> defaultPost,
        "user" -> defaultUser
      )
      smrParamValidator ! RawSmrParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSmrParams]
      msg.params.post should be(defaultPost)
    }

    "reject invalid post" in {
      val params = Map(
        "service" -> defaultService,
        "post" -> "<blah>",
        "user" -> defaultUser
      )
      smrParamValidator ! RawSmrParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSmrParams]
    }

    "accept valid user" in {
      val params = Map(
        "service" -> defaultService,
        "post" -> defaultPost,
        "user" -> defaultUser
      )
      smrParamValidator ! RawSmrParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSmrParams]
      msg.params.user should be(defaultUser)
    }

    "reject invalid user" in {
      val params = Map(
        "service" -> defaultService,
        "post" -> defaultPost,
        "user" -> "bad()=user"
      )
      smrParamValidator ! RawSmrParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSmrParams]
    }
  }
}