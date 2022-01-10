package dpla.publications

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.{Failure, Success}

class ParamValidatorTest extends AnyWordSpec with Matchers {

  val minRawParams: RawParams = RawParams(
    facets = None,
    facetSize = None,
    page = None,
    pageSize = None,
    q = None
  )

  def expectSuccess(raw: RawParams): SearchParams =
    ParamValidator.getSearchParams(raw) match {
      case Success(p) => p
      case Failure(_) => throw new RuntimeException("unexpected validation error")
    }

  "facet validator" should {
    "handle empty param" in {
      val expected = None
      val validated = expectSuccess(minRawParams).aggFields
      assert(validated == expected)
    }

    "handle valid param" in {
      val given = Some("sourceResource.subject.name")
      val expected = Some(Seq("genre"))
      val raw = minRawParams.copy(facets=given)
      val validated = expectSuccess(raw).aggFields
      assert(validated == expected)
    }

    "map all valid params" in {
      val given = Some(Seq(
        "dataProvider",
        "sourceResource.creator",
        "sourceResource.format",
        "sourceResource.language.name",
        "sourceResource.publisher",
        "sourceResource.subject.name"
      ).mkString(","))
      val expected = Seq(
        "sourceUri",
        "author",
        "medium",
        "language",
        "publisher",
        "genre"
      )
      val raw = minRawParams.copy(facets=given)
      val validated = expectSuccess(raw).aggFields.getOrElse(Seq[String]())
      validated should contain allElementsOf expected
    }

    "handle unfacetable field" in {
      val given = Some("sourceResource.title")
      val raw = minRawParams.copy(facets=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert(validated.isFailure)
    }
  }

  "page validator" should {
    "handle empty param" in {
      val expected = 1
      val validated = expectSuccess(minRawParams).page
      assert(validated == expected)
    }

    "return valid param" in {
      val given = Some("27")
      val expected = 27
      val raw = minRawParams.copy(page=given)
      val validated = expectSuccess(raw).page
      assert(validated == expected)
    }

    "handle non-int param" in {
      val given = Some("foo")
      val raw = minRawParams.copy(page=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert(validated.isFailure)
    }

    "handle out-of-range param" in {
      val given = Some("0")
      val raw = minRawParams.copy(page=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert(validated.isFailure)
    }
  }

  "page size validator" should {
    "handle empty param" in {
      val expected = 10
      val validated = expectSuccess(minRawParams).pageSize
      assert(validated == expected)
    }

    "return valid param" in {
      val given = Some("50")
      val expected = 50
      val raw = minRawParams.copy(pageSize=given)
      val validated = expectSuccess(raw).pageSize
      assert(validated == expected)
    }

    "handle non-int param" in {
      val given = Some("foo")
      val raw = minRawParams.copy(pageSize=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert(validated.isFailure)
    }

    "handle out-of-range param" in {
      val given = Some("999999")
      val raw = minRawParams.copy(pageSize=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert(validated.isFailure)
    }
  }

  "q validator" should {
    "handle empty param" in {
      val expected = None
      val validated = expectSuccess(minRawParams).q
      assert (validated == expected)
    }

    "return valid param" in {
      val given = Some("dogs")
      val expected = Some("dogs")
      val raw = minRawParams.copy(q=given)
      val validated = expectSuccess(raw).q
      assert (validated == expected)
    }

    "handle too-short param" in {
      val given = Some("d")
      val raw = minRawParams.copy(q=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert (validated.isFailure)
    }

    "handle too-long param" in {
      val given = Some(
        """
          |"Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et
          | dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip
          | ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu
          | fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt
          | mollit anim id est laborum.
          | """.stripMargin)
      val raw = minRawParams.copy(q=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert (validated.isFailure)
    }
  }
}
