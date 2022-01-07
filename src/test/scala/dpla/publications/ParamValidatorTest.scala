package dpla.publications

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ParamValidatorTest extends AnyWordSpec with Matchers {

    "page validator" should {
      "return valid param" in {
        val given = Some("27")
        val expected = 27
        val validated = ParamValidator.getSearchParams(given, None).page
        assert(validated == expected)
      }

      "handle empty param" in {
        val given = None
        val expected = 1
        val validated = ParamValidator.getSearchParams(given, None).page
        assert(validated == expected)
      }

      "handle non-int param" in {
        val given = Some("foo")
        val expected = 1
        val validated = ParamValidator.getSearchParams(given, None).page
        assert(validated == expected)
      }

      "handle out-of range-param" in {
        val given = Some("0")
        val expected = 1
        val validated = ParamValidator.getSearchParams(given, None).page
        assert(validated == expected)
      }
    }

    "page size validator" should {
      "return valid param" in {
        val given = Some("27")
        val expected = 27
        val validated = ParamValidator.getSearchParams(None, given).pageSize
        assert(validated == expected)
      }

      "handle empty param" in {
        val given = None
        val expected = 10
        val validated = ParamValidator.getSearchParams(None, given).pageSize
        assert(validated == expected)
      }

      "handle non-int param" in {
        val given = Some("foo")
        val expected = 10
        val validated = ParamValidator.getSearchParams(None, given).pageSize
        assert(validated == expected)
      }

      "handle out-of-range param" in {
        val given = Some("999999")
        val expected = 1000
        val validated = ParamValidator.getSearchParams(None, given).pageSize
        assert(validated == expected)
      }
    }
}
