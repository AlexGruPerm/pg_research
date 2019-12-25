package application
import java.util.concurrent.TimeUnit

import common._
import dbconn.{PgConnection, pgSess}
import loadconf.PgLoadConfReader
import org.slf4j.LoggerFactory
import saveresults.PgSaveResults
import testexec.PgRunCasesExec
import zio.clock.Clock
import zio.console._
import zio.{Task, ZIO, _}

/**
 * //https://zio.dev/docs/overview/overview_index
 *
 * UIO[A] — This is a type alias for ZIO[Any, Nothing, A], which represents an effect that has no requirements, and cannot fail, but can succeed with an A.
 * URIO[R, A] — This is a type alias for ZIO[R, Nothing, A], which represents an effect that requires an R, and cannot fail, but can succeed with an A.
 * Task[A] — This is a type alias for ZIO[Any, Throwable, A], which represents an effect that has no requirements, and may fail with a Throwable value, or succeed with an A.
 * RIO[R, A] — This is a type alias for ZIO[R, Throwable, A], which represents an effect that requires an R, and may fail with a Throwable value, or succeed with an A.
 * IO[E, A] — This is a type alias for ZIO[Any, E, A], which represents an effect that has no requirements, and may fail with an E, or succeed with an A.
*/
object PgResearch extends App {

  //todo: remove all println and use everywhere only _ <- putStrLn
  /**
   * todo: add timout on effects  https://zio.dev/docs/overview/overview_basic_concurrency
   * Timeout
   * ZIO lets you timeout any effect using the ZIO#timeout method,
   *
   */

  //todo: Add connection pooling - https://devcenter.heroku.com/articles/database-connection-pooling-with-scala

  def run(args: List[String]): ZIO[Console with Clock, Nothing, Int] = {
    val logger = LoggerFactory.getLogger(getClass.getName)
    val f = PgResearchLive(args).fold(
      f => {
        logger.error(s"Fail PgResearch.run f=$f msg=${f.getMessage} cause=${f.getCause}")
        f.getStackTrace.foreach(errln => logger.error(errln.toString))
        0
      },
      s => {
        println("Success");
        s.sqPgTestResult.sortBy(r => r.endTs).foreach(println);
        println("-----------------------------");
        println(s"Common duration ${s.commDurMs} ms.")
        println(s);
        s.getAgrStats
        1
      }
    )
    f
  }

  /**
   * MONITORING SESSIONS IN POSTGRES:
   * select * from pg_stat_activity p where coalesce(p.usename,'-')='prm_salary'
   * under postgres/postgres
   */

  /**
   * Get application input parameters and return Task[String] with input filename or fail
   * with Exception.
   */
  private val getInputParamFileName: List[String] => Task[CmdLineParams] = argsList =>
    if (argsList.length == 0) {
    //todo: don't forget replace succeed with fail.
      //if (argsList(0)=="run") {
        //Task.succeed(CmdLineParams("run", Some("C:\\pg_research\\src\\main\\resources\\loadconf.json"), None, None))
      /*} else if (argsList(0)=="comp"){
        Task.succeed(CmdLineParams("comp", None, Some(""), Some("")))
      }
      */
      Task.succeed(CmdLineParams("comp", None, Some("C:\\pg_research\\23_12_2019_16_13_57.json"), Some("C:\\pg_research\\23_12_2019_16_14_42.json")))
      //Task.succeed(CmdLineParams(argsList(0),...,Some("C:\\pg_research\\src\\main\\resources\\loadconf.json"),None,None))
    }
    else
      Task.succeed(CmdLineParams(argsList(0),Option(argsList(1)),Option(argsList(2)),Option(argsList(3)))/*argsList(0)*/)

  /**
   *  Check that connect credentials are valid.
  */
  private val checkDbConnectCredits : Task[PgConnectProp] => ZIO[Console, Throwable, Unit] = TdbConProps =>
    for {
      dbConProps <- TdbConProps
      pgSes: pgSess <- (new PgConnection).sess(0,dbConProps)
      _ <- putStrLn(s"Connection opened - ${!pgSes.sess.isClosed}")
    } yield ()


  /**
   *  Get max_connections from pg config
  */
  private val checkDbMaxConnections : Task[PgConnectProp] => ZIO[Console, Throwable, Unit] = TdbConProps =>
    for {
      dbConProps <- TdbConProps
      settings :PgSettings <- (new PgConnection).getMaxConns(dbConProps)
      _ <- putStrLn(s"Config : max_connections = ${settings.maxConn} conf : ${settings.sourceFile}")
    } yield ()


  private val PgResearchLive : List[String] => ZIO[Console with Clock, Throwable, PgTestResultAgr] =
    args => for {
      params :CmdLineParams <- getInputParamFileName(args)
      _ <- putStrLn(s"Begin with config file ${params.confFile}")
      pgcp = PgLoadConfReader.getDbConnectionProps(params.confFile.getOrElse(""))
      dbConProps :PgConnectProp <- pgcp
      sqLoadConf :Seq[PgLoadConf] <- PgLoadConfReader.getLoadItems(params.confFile.getOrElse(""))
      _ <- checkDbConnectCredits(pgcp)
      _ <- checkDbMaxConnections(pgcp)
      runProperties :PgRunProp <- PgLoadConfReader.getPgRunProp(params.confFile.getOrElse(""))
      _ <- putStrLn(s"Running in mode - ${runProperties.runAs} iterations = ${runProperties.repeat}")
      tBegin <- clock.currentTime(TimeUnit.MILLISECONDS)
      sqTestResults :Seq[PgTestResult] <- PgRunCasesExec.run(runProperties.runAs, runProperties, dbConProps, sqLoadConf)
      tEnd <- clock.currentTime(TimeUnit.MILLISECONDS)
      testAgrResult :PgTestResultAgr = PgTestResultAgr(sqTestResults,tEnd-tBegin,runProperties)
      saveResStatus <- PgSaveResults.saveResIntoFiles(testAgrResult)
      _ <- putStrLn(s"Results saved in file $saveResStatus")
    } yield testAgrResult

}