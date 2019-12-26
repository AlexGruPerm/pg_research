package application

import java.util.concurrent.TimeUnit

import common.{PgConnectProp, PgLoadConf, PgRunProp, PgTestResult, PgTestResultAgr}
import dbconn.CommonDbCheck
import loadconf.PgLoadConfReader
import saveresults.PgSaveResults
import testexec.PgRunCasesExec
import zio.{Task, ZIO, clock}
import zio.clock.Clock
import zio.console.{Console, putStrLn}

object PgResearchRun {

  /**
   * Code executed for run option. Execute queries in db.
   */
   val run : Task[CmdLineParams] => ZIO[Console with Clock, Throwable, Int] = clp =>
    for {
      params :CmdLineParams  <- clp
      _ <- putStrLn(s"Begin with config file ${params.confFile}")
      pgcp = PgLoadConfReader.getDbConnectionProps(params.confFile.getOrElse(""))
      dbConProps :PgConnectProp <- pgcp
      sqLoadConf :Seq[PgLoadConf] <- PgLoadConfReader.getLoadItems(params.confFile.getOrElse(""))
      _ <- CommonDbCheck.checkDbConnectCredits(pgcp)
      _ <- CommonDbCheck.checkDbMaxConnections(pgcp)
      runProperties :PgRunProp <- PgLoadConfReader.getPgRunProp(params.confFile.getOrElse(""))
      _ <- putStrLn(s"Running in mode - ${runProperties.runAs} iterations = ${runProperties.repeat}")
      tBegin <- clock.currentTime(TimeUnit.MILLISECONDS)
      sqTestResults :Seq[PgTestResult] <- PgRunCasesExec.run(runProperties.runAs, runProperties, dbConProps, sqLoadConf)
      tEnd <- clock.currentTime(TimeUnit.MILLISECONDS)
      testAgrResult :PgTestResultAgr = PgTestResultAgr(sqTestResults,tEnd-tBegin,runProperties)
      saveResStatus <- PgSaveResults.saveResIntoFiles(testAgrResult)
      _ <- putStrLn(s"Results saved in file $saveResStatus")
      _ <- putStrLn(testAgrResult.toString)
      _ <- putStrLn(s"Common duration ${testAgrResult.commDurMs} ms")
      _ <- putStrLn(s"-----------------------------")
    } yield 1

}