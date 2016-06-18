package io.fintrospect.parameters

import java.time.LocalDate

import com.twitter.finagle.http.Request
import io.fintrospect.parameters.InvalidParameter.{Invalid, Missing}
import org.scalatest._

class ExtractorTest extends FunSpec with ShouldMatchers {

  case class Example(a: Option[String], b: Option[String], c: Int)

  case class WrappedExample(d: Option[Example], e: Int)

  describe("Extractable") {
    it("does not short circuit if all parameters in a for comprehension are optional") {
      val ex = Extractor.mk {
        request: Request => for {
          req <- Query.optional.int("req") <--? request
          opt <- Query.optional.int("optional") <--? request
        } yield (req, opt)
      }

      ex <--? Request("/") shouldBe Extracted((None, None))
    }

    it("does not short circuit if last parameter in a for comprehension is optional") {
      val ex = Extractor.mk {
        request: Request => for {
          req <- Query.required.int("req") <--? request
          opt <- Query.optional.int("optional") <--? request
        } yield (req, opt)
      }

      ex <--? Request("/?req=123") shouldBe Extracted((Some(123), None))
    }

    describe("non-embedded extraction") {
      val int = Query.required.int("name3")
      val c = Extractor.mk {
        request: Request => for {
          name3 <- int.extract(request)
          name1 <- Query.optional.string("name1").extract(request)
          name2 <- Query.optional.string("name2").extract(request)
        } yield Example(name1, name2, name3.get)
      }

      it("successfully extracts when all parameters present") {
        c <--? Request("/?name1=query1&name2=rwer&name3=12") shouldBe Extracted(Example(Some("query1"), Some("rwer"), 12))
      }

      it("successfully extracts when only optional parameters missing") {
        c <--? Request("/?name3=123") shouldBe Extracted(Example(None, None, 123))
      }

      it("reports error when not all parameters present") {
        c <--? Request("/?name1=query1") shouldBe ExtractionFailed(Missing(int))
      }
    }

    it("validation error between parameters") {

      case class Range(startDate: LocalDate, middleDate: Option[LocalDate], endDate: LocalDate)

      val start = Query.optional.localDate("start")
      val middle = Query.optional.localDate("middle")
      val end = Query.required.localDate("end")

      val c = Extractor.mk {
        request: Request => {
          for {
            startDate <- start <--? request
            middleDate <- middle <--?(request, "not after start", (i: LocalDate) => i.isAfter(startDate.get))
            endDate <- end <--?(request, "not after start", e => startDate.map(s => e.isAfter(s)).getOrElse(true))
          } yield Some(Range(startDate.get, middleDate, endDate.get))
        }
      }

      c <--? Request("/?start=2002-01-01&end=2001-01-01") shouldBe ExtractionFailed(InvalidParameter(end, "not after start"))
    }

    describe("can embed extractables") {
      val innerInt = Query.required.int("innerInt")
      val outerInt = Query.required.int("outerInt")
      val inner = Extractor.mk {
        request: Request => for {
          name3 <- innerInt.extract(request)
          name1 <- Query.optional.string("name1").extract(request)
          name2 <- Query.optional.string("name2").extract(request)
        } yield Example(name1, name2, name3.get)
      }

      val outer = Extractor.mk {
        request: Request => for {
          name4 <- outerInt <--? request
          inner <- inner <--? request
        } yield WrappedExample(inner, name4.get)
      }

      it("success") {
        outer <--? Request("/?innerInt=123&outerInt=1") shouldBe Extracted(WrappedExample(Some(Example(None, None, 123)), 1))
      }

      it("inner extract fails reports only inner error") {
        outer <--? Request("/?outerInt=123") shouldBe ExtractionFailed(Missing(innerInt))
      }
      it("outer extract fails reports only outer error") {
        outer <--? Request("/?innerInt=123") shouldBe ExtractionFailed(Missing(outerInt))
      }
    }

    describe("falling back to default value") {
      it("Extracted") {
        Extracted(true).orDefault(false) shouldBe Extracted(true)
      }
      it("NotProvided") {
        NotProvided.orDefault(true) shouldBe Extracted(true)
      }
      it("ExtractionFailed") {
        val param = Query.required.string("param")
        ExtractionFailed(Invalid(param)).orDefault(true) shouldBe ExtractionFailed(Invalid(param))

      }
    }

    describe("misc methods") {
      val invalid = Invalid(Query.optional.string("bob"))
      val missing = Missing(Query.optional.string("bob"))
      it("flatten") {
        Extraction.flatten(NotProvided) shouldBe NotProvided
        Extraction.flatten(Extracted(None)) shouldBe NotProvided
        Extraction.flatten(Extracted(Some(1))) shouldBe Extracted(1)
        Extraction.flatten(ExtractionFailed(Seq(invalid))) shouldBe ExtractionFailed(Seq(invalid))
      }
      it("combine") {
        Extraction.combine(Seq(NotProvided, NotProvided)) shouldBe NotProvided
        Extraction.combine(Seq(NotProvided, Extracted(1))) shouldBe NotProvided
        Extraction.combine(Seq(NotProvided, Extracted(1), ExtractionFailed(missing), ExtractionFailed(invalid))) shouldBe ExtractionFailed(Seq(missing, invalid))
      }
    }

  }

}