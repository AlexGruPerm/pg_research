package saveresults

import common.PgTestResult
import zio._

object PgSaveResultAsJson {

  val saveResIntoFile : (String,Seq[PgTestResult]) => Task[Boolean] = (outputFileName, sqPgTestResult) => {
    ???
  }

}
