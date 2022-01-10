package dpla.publications

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ParamValidatorTest extends AnyWordSpec with Matchers {

  val minRawParams: RawParams = RawParams(
    facets = None,
    page = None,
    pageSize = None,
    q = None
  )

    "page validator" should {
      "handle empty param" in {
        val expected = 1
        val validated = ParamValidator.getSearchParams(minRawParams).page
        assert(validated == expected)
      }

      "return valid param" in {
        val given = Some("27")
        val expected = 27
        val raw = minRawParams.copy(page=given)
        val validated = ParamValidator.getSearchParams(raw).page
        assert(validated == expected)
      }

      "handle non-int param" in {
        val given = Some("foo")
        val expected = 1
        val raw = minRawParams.copy(page=given)
        val validated = ParamValidator.getSearchParams(raw).page
        assert(validated == expected)
      }

      "handle out-of range-param" in {
        val given = Some("0")
        val expected = 1
        val raw = minRawParams.copy(page=given)
        val validated = ParamValidator.getSearchParams(raw).page
        assert(validated == expected)
      }
    }

    "page size validator" should {
      "handle empty param" in {
        val expected = 10
        val validated = ParamValidator.getSearchParams(minRawParams).pageSize
        assert(validated == expected)
      }

      "return valid param" in {
        val given = Some("50")
        val expected = 50
        val raw = minRawParams.copy(pageSize=given)
        val validated = ParamValidator.getSearchParams(raw).pageSize
        assert(validated == expected)
      }

      "handle non-int param" in {
        val given = Some("foo")
        val expected = 10
        val raw = minRawParams.copy(pageSize=given)
        val validated = ParamValidator.getSearchParams(raw).pageSize
        assert(validated == expected)
      }

      "handle out-of-range param" in {
        val given = Some("999999")
        val expected = 1000
        val raw = minRawParams.copy(pageSize=given)
        val validated = ParamValidator.getSearchParams(raw).pageSize
        assert(validated == expected)
      }
    }

    "q validator" should {
      "handle empty param" in {
        val given = None
        val expected = None
        val validated = ParamValidator.getSearchParams(minRawParams).q
        assert (validated == expected)
      }

      "return valid param" in {
        val given = Some("dogs")
        val expected = Some("dogs")
        val raw = minRawParams.copy(q=given)
        val validated = ParamValidator.getSearchParams(raw).q
        assert (validated == expected)
      }

      "hand out-of-range param" in {
        val given = Some("d")
        val expected = None
        val raw = minRawParams.copy(q=given)
        val validated = ParamValidator.getSearchParams(raw).q
        assert (validated == expected)
      }
    }
}
