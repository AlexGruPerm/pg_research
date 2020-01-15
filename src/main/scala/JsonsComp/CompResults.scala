package JsonsComp

import io.circe._
import io.circe.generic.semiauto._
import zio.Task




/**
 * Case class for result of one test execution.
*/
case class IterTestSingleRes(
                              iterNum: Int,
                              pid: Long,
                              testNum: Int,
                              testName: String,
                              procName: String,
                              startTs: Long,
                              endTs: Long,
                              curColumnsSize: Int,
                              durExecMs: Long,
                              durFetchMs: Long,
                              durTotalMs: Long,
                              numRows: Int
                            )

/**
 * All iterations and all tests results. One input file.
*/
case class OneFileTests(
                        runType: String,
                        cntDistPids: Int,
                        CommonDurMs: Long,
                        sumExecDurMs: Long,
                        sumFetchDurMs: Long,
                        sumTotalDurMs: Long,
                        test_results: Seq[IterTestSingleRes]
                      )

object implDecEnc {
  implicit val IterTestSingleResDecoder: Decoder[IterTestSingleRes] = deriveDecoder
  implicit val IterTestSingleResEncoder: Encoder[IterTestSingleRes] = deriveEncoder

  implicit val OneFileTestsDecoder: Decoder[OneFileTests] = deriveDecoder
  implicit val OneFileTestsEncoder: Encoder[OneFileTests] = deriveEncoder
}

/**
 *  Results of comparison of 2 input json files.
 */
case class SummaryOfComp(
                     f1name: String,
                     f1:     Task[OneFileTests],
                     f2name: String,
                     f2:     Task[OneFileTests]
                   )

object SummaryOfComp {

  private val JsonToOneFileTests : Json => Task[OneFileTests] = js => {
    js.as[OneFileTests](implDecEnc.OneFileTestsDecoder).swap match {
      case   Left(sq) => Task.succeed(sq)
      case   Right(failure) =>  Task.fail (new Exception (s"Invalid json in input file. $failure"))
    }
  }

  def apply(j1FileName :String, j1: Json,
            j2FileName :String, j2: Json) =
    new SummaryOfComp(
      j1FileName, JsonToOneFileTests(j1),
      j2FileName, JsonToOneFileTests(j2)
    )
}
