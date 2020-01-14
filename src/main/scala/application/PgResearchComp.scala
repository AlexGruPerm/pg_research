package application

import java.util.concurrent.TimeUnit

import JsonsComp.JsonsCompare
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
      _ <- putStrLn(s"Begin comparison json files -------------------------")
      _ <- putStrLn(s"File (1) : ${params.inputFile1}")
      _ <- putStrLn(s"File (2) : ${params.inputFile2}")
      tBegin  <- clock.currentTime(TimeUnit.MILLISECONDS)
      resSummaryOfComp  <- JsonsCompare.compare(
        params.inputFile1.getOrElse("Error: empty filename"),
        params.inputFile2.getOrElse("Error: empty filename"))
      tEndRP  <- clock.currentTime(TimeUnit.MILLISECONDS)
      durationMs :Long = (tEndRP-tBegin)
      _ <- putStrLn(s"(1) ${resSummaryOfComp.f1name} resSummaryOfComp = ${resSummaryOfComp.f1}")
      _ <- putStrLn(s"(2) ${resSummaryOfComp.f2name} resSummaryOfComp = ${resSummaryOfComp.f2}")
      _ <- putStrLn(s"Common (jsons read-parse) duration $durationMs ms.")
      saveResStatus <- JsonsCompare.saveResIntoFile(resSummaryOfComp)
      _ <- putStrLn(s"result saved into file $saveResStatus.!!!")
      _ <- putStrLn(s"-----------------------------")
    } yield 1

}