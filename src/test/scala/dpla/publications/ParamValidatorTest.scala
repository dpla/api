package dpla.publications

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.{Failure, Success}

class ParamValidatorTest extends AnyWordSpec with Matchers {

  val minRawParams: RawParams = RawParams(
    creator = None,
    dataProvider = None,
    date = None,
    description = None,
    facets = None,
    facetSize = None,
    format = None,
    isShownAt = None,
    language = None,
    `object` = None,
    page = None,
    pageSize = None,
    publisher = None,
    q = None,
    subject = None,
    subtitle = None,
    title = None
  )

  def expectSuccess(raw: RawParams): SearchParams =
    ParamValidator.getSearchParams(raw) match {
      case Success(p) => p
      case Failure(_) => throw new RuntimeException("unexpected validation error")
    }

  "dataProvider validator" should {
    "handle empty param" in {
      val expected = None
      val validated = expectSuccess(minRawParams).dataProvider
      assert(validated == expected)
    }

    "return valid param" in {
      val given = Some("https://standardebooks.org")
      val expected = Some("https://standardebooks.org")
      val raw = minRawParams.copy(dataProvider = given)
      val validated = expectSuccess(raw).dataProvider
      assert(validated == expected)
    }

    "handle invalid URL" in {
      val given = Some("standardebooks")
      val raw = minRawParams.copy(dataProvider = given)
      val validated = ParamValidator.getSearchParams(raw)
      assert(validated.isFailure)
    }
  }

  "creator validator" should {
    "handle empty param" in {
      val expected = None
      val validated = expectSuccess(minRawParams).creator
      assert(validated == expected)
    }

    "return valid param" in {
      val given = Some("Jules Verne")
      val expected = Some("Jules Verne")
      val raw = minRawParams.copy(creator = given)
      val validated = expectSuccess(raw).creator
      assert(validated == expected)
    }

    "handle too-short param" in {
      val given = Some("d")
      val raw = minRawParams.copy(creator = given)
      val validated = ParamValidator.getSearchParams(raw)
      assert(validated.isFailure)
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
      val raw = minRawParams.copy(creator=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert (validated.isFailure)
    }
  }

    "date validator" should {
      "handle empty param" in {
        val expected = None
        val validated = expectSuccess(minRawParams).date
        assert (validated == expected)
      }

      "return valid param" in {
        val given = Some("2002")
        val expected = Some("2002")
        val raw = minRawParams.copy(date=given)
        val validated = expectSuccess(raw).date
        assert (validated == expected)
      }

      "handle too-short param" in {
        val given = Some("1")
        val raw = minRawParams.copy(date=given)
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
      val raw = minRawParams.copy(date=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert (validated.isFailure)
    }
  }

  "description validator" should {
    "handle empty param" in {
      val expected = None
      val validated = expectSuccess(minRawParams).description
      assert (validated == expected)
    }

    "return valid param" in {
      val given = Some("dogs")
      val expected = Some("dogs")
      val raw = minRawParams.copy(description=given)
      val validated = expectSuccess(raw).description
      assert (validated == expected)
    }

    "handle too-short param" in {
      val given = Some("d")
      val raw = minRawParams.copy(description=given)
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
      val raw = minRawParams.copy(description=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert (validated.isFailure)
    }
  }

  "facet validator" should {
    "handle empty param" in {
      val expected = None
      val validated = expectSuccess(minRawParams).facets
      assert(validated == expected)
    }

    "handle valid param" in {
      val given = Some("dataProvider")
      val expected = Some(Seq("dataProvider"))
      val raw = minRawParams.copy(facets=given)
      val validated = expectSuccess(raw).facets
      assert(validated == expected)
    }

    "handle unfacetable field" in {
      val given = Some("sourceResource.title")
      val raw = minRawParams.copy(facets=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert(validated.isFailure)
    }
  }

  "facet size validator" should {
    "handle empty param" in {
      val expected = 50
      val validated = expectSuccess(minRawParams).facetSize
      assert(validated == expected)
    }

    "return valid param" in {
      val given = Some("30")
      val expected = 30
      val raw = minRawParams.copy(facetSize=given)
      val validated = expectSuccess(raw).facetSize
      assert(validated == expected)
    }

    "handle non-int param" in {
      val given = Some("foo")
      val raw = minRawParams.copy(page=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert(validated.isFailure)
    }

    "handle out-of-range param" in {
      val given = Some("9999")
      val raw = minRawParams.copy(facetSize=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert(validated.isFailure)
    }
  }

  "format validator" should {
    "handle empty param" in {
      val expected = None
      val validated = expectSuccess(minRawParams).format
      assert (validated == expected)
    }

    "return valid param" in {
      val given = Some("article")
      val expected = Some("article")
      val raw = minRawParams.copy(format=given)
      val validated = expectSuccess(raw).format
      assert (validated == expected)
    }

    "handle too-short param" in {
      val given = Some("d")
      val raw = minRawParams.copy(format=given)
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
      val raw = minRawParams.copy(format=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert (validated.isFailure)
    }
  }

  "isShownAt validator" should {
    "handle empty param" in {
      val expected = None
      val validated = expectSuccess(minRawParams).isShownAt
      assert(validated == expected)
    }

    "return valid param" in {
      val given = Some("https://standardebooks.org/ebooks/j-s-fletcher/the-charing-cross-mystery")
      val expected = Some("https://standardebooks.org/ebooks/j-s-fletcher/the-charing-cross-mystery")
      val raw = minRawParams.copy(isShownAt = given)
      val validated = expectSuccess(raw).isShownAt
      assert(validated == expected)
    }

    "handle invalid URL" in {
      val given = Some("the-charing-cross-mystery")
      val raw = minRawParams.copy(isShownAt = given)
      val validated = ParamValidator.getSearchParams(raw)
      assert(validated.isFailure)
    }
  }

  "language validator" should {
    "handle empty param" in {
      val expected = None
      val validated = expectSuccess(minRawParams).language
      assert (validated == expected)
    }

    "return valid param" in {
      val given = Some("fr")
      val expected = Some("fr")
      val raw = minRawParams.copy(language=given)
      val validated = expectSuccess(raw).language
      assert (validated == expected)
    }

    "handle too-short param" in {
      val given = Some("d")
      val raw = minRawParams.copy(language=given)
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
      val raw = minRawParams.copy(language=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert (validated.isFailure)
    }
  }

  "object validator" should {
    "handle empty param" in {
      val expected = None
      val validated = expectSuccess(minRawParams).`object`
      assert(validated == expected)
    }

    "return valid param" in {
      val given = Some("http://payload-permanent-address.dp.la")
      val expected = Some("http://payload-permanent-address.dp.la")
      val raw = minRawParams.copy(`object` = given)
      val validated = expectSuccess(raw).`object`
      assert(validated == expected)
    }

    "handle invalid URL" in {
      val given = Some("http/payload-permanent-address.dp.la")
      val raw = minRawParams.copy(`object` = given)
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

  "publisher validator" should {
    "handle empty param" in {
      val expected = None
      val validated = expectSuccess(minRawParams).publisher
      assert (validated == expected)
    }

    "return valid param" in {
      val given = Some("Penguin")
      val expected = Some("Penguin")
      val raw = minRawParams.copy(publisher=given)
      val validated = expectSuccess(raw).publisher
      assert (validated == expected)
    }

    "handle too-short param" in {
      val given = Some("d")
      val raw = minRawParams.copy(publisher=given)
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
      val raw = minRawParams.copy(publisher=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert (validated.isFailure)
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

  "subject validator" should {
    "handle empty param" in {
      val expected = None
      val validated = expectSuccess(minRawParams).subject
      assert (validated == expected)
    }

    "return valid param" in {
      val given = Some("dogs")
      val expected = Some("dogs")
      val raw = minRawParams.copy(subject=given)
      val validated = expectSuccess(raw).subject
      assert (validated == expected)
    }

    "handle too-short param" in {
      val given = Some("d")
      val raw = minRawParams.copy(subject=given)
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
      val raw = minRawParams.copy(subject=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert (validated.isFailure)
    }
  }

  "subtitle validator" should {
    "handle empty param" in {
      val expected = None
      val validated = expectSuccess(minRawParams).subtitle
      assert (validated == expected)
    }

    "return valid param" in {
      val given = Some("A play in three acts")
      val expected = Some("A play in three acts")
      val raw = minRawParams.copy(subtitle=given)
      val validated = expectSuccess(raw).subtitle
      assert (validated == expected)
    }

    "handle too-short param" in {
      val given = Some("d")
      val raw = minRawParams.copy(subtitle=given)
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
      val raw = minRawParams.copy(subtitle=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert (validated.isFailure)
    }
  }

  "title validator" should {
    "handle empty param" in {
      val expected = None
      val validated = expectSuccess(minRawParams).title
      assert (validated == expected)
    }

    "return valid param" in {
      val given = Some("The Scarlet Letter")
      val expected = Some("The Scarlet Letter")
      val raw = minRawParams.copy(title=given)
      val validated = expectSuccess(raw).title
      assert (validated == expected)
    }

    "handle too-short param" in {
      val given = Some("d")
      val raw = minRawParams.copy(title=given)
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
      val raw = minRawParams.copy(title=given)
      val validated = ParamValidator.getSearchParams(raw)
      assert (validated.isFailure)
    }
  }
}
