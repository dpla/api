package dpla.api.v2.search

import java.util.concurrent.{Semaphore, TimeUnit}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

/** Limits concurrent execution of Futures using a semaphore.
  *
  * IMPORTANT: The `apply` method uses `tryAcquire` with a timeout, which BLOCKS
  * the calling thread for up to `timeoutSeconds`. In Akka actor contexts, this
  * means actor threads may be blocked. This is intentional to provide
  * backpressure when the system is overloaded, but callers should be aware of
  * this behavior.
  *
  * For high-throughput scenarios, consider:
  *   - Using a dedicated blocking dispatcher for operations that use this
  *     limiter
  *   - Tuning `maxConcurrent` and `timeoutSeconds` based on your workload
  *   - Monitoring permit acquisition times
  *
  * @param maxConcurrent
  *   Maximum number of concurrent operations allowed
  * @param timeoutSeconds
  *   Maximum time to wait for a permit before failing
  */
class ConcurrencyLimiter(
    val maxConcurrent: Int,
    val timeoutSeconds: Long
) {
  require(
    maxConcurrent > 0,
    s"maxConcurrent must be positive, got: $maxConcurrent"
  )
  require(
    timeoutSeconds > 0,
    s"timeoutSeconds must be positive, got: $timeoutSeconds"
  )

  private val semaphore = new Semaphore(maxConcurrent)

  /** Wraps a Future with concurrency limiting.
    *
    *   - Attempts to acquire a permit with timeout
    *   - If permit acquired, executes the Future and releases permit on
    *     completion
    *   - If timeout exceeded, returns a failed Future immediately
    *   - Ensures permit is released even if Future construction throws
    *
    * @param f
    *   The Future to execute (call-by-name, evaluated only if permit acquired)
    * @param ec
    *   ExecutionContext for Future callbacks
    * @return
    *   The wrapped Future, or a failed Future if permit couldn't be acquired
    */
  def apply[T](f: => Future[T])(implicit ec: ExecutionContext): Future[T] = {
    val acquired = try {
      semaphore.tryAcquire(timeoutSeconds, TimeUnit.SECONDS)
    } catch {
      case e: InterruptedException =>
        Thread.currentThread().interrupt()
        return Future.failed(e)
    }

    if (!acquired) {
      Future.failed(
        ConcurrencyLimitExceeded(
          maxConcurrent = maxConcurrent,
          timeoutSeconds = timeoutSeconds
        )
      )
    } else {
      try {
        val future = f
        future.andThen { case _ => semaphore.release() }(ec)
      } catch {
        case NonFatal(e) =>
          semaphore.release()
          Future.failed(e)
      }
    }
  }

  /** Returns the number of permits currently available. Useful for monitoring
    * and debugging.
    */
  def availablePermits: Int = semaphore.availablePermits()
}

/** Exception thrown when a concurrency limit is exceeded and the timeout
  * expires.
  */
case class ConcurrencyLimitExceeded(
    maxConcurrent: Int,
    timeoutSeconds: Long
) extends RuntimeException(
      s"Concurrency limit ($maxConcurrent) exceeded, " +
        s"timed out after ${timeoutSeconds}s waiting for permit"
    )
