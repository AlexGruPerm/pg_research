package testexec
import zio._

import common.{PgLoadConf, PgTestResult}

object PgTestExecuter {

  val exec :PgLoadConf => Task[PgTestResult] = lc => {
    Task.succeed(
      PgTestResult(lc,duration = lc.testNum*100L,numRows = 100)
    )
  }

}
