package application

import java.util.concurrent.TimeUnit

import zio.clock.Clock
import zio.console.{Console, putStrLn}
import zio.{Task, ZIO, clock}

object PgResearchComp {

  /**
   * Code executed for comp option. Compare 2 input json files.
   */
   val comp : Task[CmdLineParams] => ZIO[Console with Clock, Throwable, Int] = clp =>
    for {
      params :CmdLineParams  <- clp
      _ <- putStrLn(s"Begin comparison.")
      _ <- putStrLn(s"First file : ${params.inputFile1}")
      _ <- putStrLn(s"Second file : ${params.inputFile2}")

      tBegin  <- clock.currentTime(TimeUnit.MILLISECONDS)
      //sqTestResults :Seq[PgTestResult] <- PgRunCasesExec.run(runProperties.runAs, runProperties, dbConProps, sqLoadConf)
      tEnd  <- clock.currentTime(TimeUnit.MILLISECONDS)
      durationMs :Long = (tEnd-tBegin)
      //saveResStatus <- PgSaveResults.saveResIntoFiles(testAgrResult)
      //_ <- putStrLn(s"Results saved in file $saveResStatus")
      //_ <- putStrLn(testAgrResult.toString)
      _ <- putStrLn(s"Common duration $durationMs ms.")
      _ <- putStrLn(s"-----------------------------")
    } yield 1

}