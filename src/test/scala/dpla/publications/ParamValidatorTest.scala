package dpla.publications

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ParamValidatorTest extends AnyWordSpec with Matchers {

    "page validator" should {
      "return valid param" in {
        val given = Some("27")
        val expected = 27
        val validated = ParamValidator.getSearchParams(given, None, None).page
        assert(validated == expected)
      }

      "handle empty param" in {
        val given = None
        val expected = 1
        val validated = ParamValidator.getSearchParams(given, None, None).page
        assert(validated == expected)
      }

      "handle non-int param" in {
        val given = Some("foo")
        val expected = 1
        val validated = ParamValidator.getSearchParams(given, None, None).page
        assert(validated == expected)
      }

      "handle out-of range-param" in {
        val given = Some("0")
        val expected = 1
        val validated = ParamValidator.getSearchParams(given, None, None).page
        assert(validated == expected)
      }
    }

    "page size validator" should {
      "return valid param" in {
        val given = Some("27")
        val expected = 27
        val validated = ParamValidator.getSearchParams(None, given, None).pageSize
        assert(validated == expected)
      }

      "handle empty param" in {
        val given = None
        val expected = 10
        val validated = ParamValidator.getSearchParams(None, given, None).pageSize
        assert(validated == expected)
      }

      "handle non-int param" in {
        val given = Some("foo")
        val expected = 10
        val validated = ParamValidator.getSearchParams(None, given, None).pageSize
        assert(validated == expected)
      }

      "handle out-of-range param" in {
        val given = Some("999999")
        val expected = 1000
        val validated = ParamValidator.getSearchParams(None, given, None).pageSize
        assert(validated == expected)
      }
    }

    "q validator" should {
      "return valid param" in {
        val given = Some("dogs")
        val expected = Some("dogs")
        val validated = ParamValidator.getSearchParams(None, None, given).q
        assert (validated == expected)
      }

      "handle empty param" in {
        val given = None
        val expected = None
        val validated = ParamValidator.getSearchParams(None, None, given).q
        assert (validated == expected)
      }

      "hand out-of-range param" in {
        val given = Some("d")
        val expected = None
        val validated = ParamValidator.getSearchParams(None, None, given).q
        assert (validated == expected)
      }
    }
}
