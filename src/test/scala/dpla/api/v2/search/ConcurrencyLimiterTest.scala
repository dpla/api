package dpla.api.v2.search

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.time.{Milliseconds, Seconds, Span}

import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global

class ConcurrencyLimiterTest
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with Eventually {

  // Configure patience for async assertions
  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(
      timeout = Span(5, Seconds),
      interval = Span(50, Milliseconds)
    )

  "ConcurrencyLimiter" should {

    "allow a successful Future to complete and release permit" in {
      val limiter =
        new ConcurrencyLimiter(maxConcurrent = 2, timeoutSeconds = 1)

      limiter.availablePermits shouldBe 2

      val result = limiter(Future.successful("success"))

      whenReady(result) { value =>
        value shouldBe "success"
      }

      // Permit should be released after Future completes
      eventually {
        limiter.availablePermits shouldBe 2
      }
    }

    "allow a failed Future to complete and release permit" in {
      val limiter =
        new ConcurrencyLimiter(maxConcurrent = 2, timeoutSeconds = 1)
      val error = new RuntimeException("test error")

      limiter.availablePermits shouldBe 2

      val result = limiter(Future.failed(error))

      whenReady(result.failed) { ex =>
        ex shouldBe error
      }

      // Permit should be released even on failure
      eventually {
        limiter.availablePermits shouldBe 2
      }
    }

    "release permit if Future construction throws" in {
      val limiter =
        new ConcurrencyLimiter(maxConcurrent = 2, timeoutSeconds = 1)
      val error = new RuntimeException("construction error")

      limiter.availablePermits shouldBe 2

      val result = limiter {
        throw error
      }

      whenReady(result.failed) { ex =>
        ex shouldBe error
      }

      // Permit should be released even when Future construction throws
      limiter.availablePermits shouldBe 2
    }

    "limit concurrent executions to maxConcurrent" in {
      val limiter =
        new ConcurrencyLimiter(maxConcurrent = 2, timeoutSeconds = 1)
      val promises = (1 to 3).map(_ => Promise[String]())

      limiter.availablePermits shouldBe 2

      // Start two futures that won't complete until we say so
      val future1 = limiter(promises(0).future)
      val future2 = limiter(promises(1).future)

      // Both should have acquired permits
      eventually {
        limiter.availablePermits shouldBe 0
      }

      // Third request should fail because limit is reached and timeout expires
      val future3 = limiter(promises(2).future)

      whenReady(future3.failed) { ex =>
        ex shouldBe a[ConcurrencyLimitExceeded]
        val cle = ex.asInstanceOf[ConcurrencyLimitExceeded]
        cle.maxConcurrent shouldBe 2
        cle.timeoutSeconds shouldBe 1
      }

      // Complete the first two futures
      promises(0).success("result1")
      promises(1).success("result2")

      whenReady(future1) { _ shouldBe "result1" }
      whenReady(future2) { _ shouldBe "result2" }

      // Permits should be released
      eventually {
        limiter.availablePermits shouldBe 2
      }
    }

    "allow new requests after permits are released" in {
      val limiter =
        new ConcurrencyLimiter(maxConcurrent = 1, timeoutSeconds = 1)
      val promise1 = Promise[String]()

      // First request acquires the only permit
      val future1 = limiter(promise1.future)
      eventually {
        limiter.availablePermits shouldBe 0
      }

      // Complete first request
      promise1.success("first")
      whenReady(future1) { _ shouldBe "first" }

      // Permit should be released
      eventually {
        limiter.availablePermits shouldBe 1
      }

      // Second request should succeed
      val future2 = limiter(Future.successful("second"))
      whenReady(future2) { _ shouldBe "second" }
    }

    "reject construction with non-positive maxConcurrent" in {
      an[IllegalArgumentException] should be thrownBy {
        new ConcurrencyLimiter(maxConcurrent = 0, timeoutSeconds = 1)
      }
      an[IllegalArgumentException] should be thrownBy {
        new ConcurrencyLimiter(maxConcurrent = -1, timeoutSeconds = 1)
      }
    }

    "reject construction with non-positive timeoutSeconds" in {
      an[IllegalArgumentException] should be thrownBy {
        new ConcurrencyLimiter(maxConcurrent = 1, timeoutSeconds = 0)
      }
      an[IllegalArgumentException] should be thrownBy {
        new ConcurrencyLimiter(maxConcurrent = 1, timeoutSeconds = -1)
      }
    }
  }

  "ConcurrencyLimitExceeded" should {

    "contain useful error message" in {
      val ex = ConcurrencyLimitExceeded(maxConcurrent = 32, timeoutSeconds = 5)
      ex.getMessage should include("32")
      ex.getMessage should include("5")
      ex.getMessage should include("Concurrency limit")
    }

    "expose maxConcurrent and timeoutSeconds" in {
      val ex = ConcurrencyLimitExceeded(maxConcurrent = 32, timeoutSeconds = 5)
      ex.maxConcurrent shouldBe 32
      ex.timeoutSeconds shouldBe 5
    }
  }
}
