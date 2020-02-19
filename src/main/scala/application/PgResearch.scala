package application
import org.slf4j.LoggerFactory
import zio.clock.Clock
import zio.console._
import zio.{Task, ZIO, _}

/**
 * https://zio.dev/docs/overview/overview_index
 * Apache DBCP : https://devcenter.heroku.com/articles/database-connection-pooling-with-scala
 *  todo: add timout on effects  https://zio.dev/docs/overview/overview_basic_concurrency
 *    Timeout ZIO lets you timeout any effect using the ZIO#timeout method
*/
object PgResearch extends App {

  def run(args: List[String]): ZIO[Console with Clock, Nothing, Int] = {
    val logger = LoggerFactory.getLogger(getClass.getName)
    PgResearchLive(args).fold(
      f => {
        logger.error(s"Fail PgResearch.run f=$f msg=${f.getMessage} cause=${f.getCause}")
        f.getStackTrace.foreach(_ => logger.error(toString))
        0
      },
      s => {
        logger.info("Success")
        1
      }
    )
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
  /**
   * DC   172.17.100.53
   * LXC  10.241.5.234
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

      Task.succeed(CmdLineParams("comp", None,
        Some("C:\\pg_research\\23_01_2020_14_52_16.json"),
        Some("C:\\pg_research\\23_01_2020_14_54_06.json")))

    }
    else
      Task.succeed(CmdLineParams(argsList(0),Option(argsList(1)),Option(argsList(2)),Option(argsList(3)))/*argsList(0)*/)

  /**
   * Main application flow.
   * Read command line parameters and select appropriate branch of code.
  */
  private val PgResearchLive: List[String] => ZIO[Console with Clock, Throwable, Int] = args => {
    val params: Task[CmdLineParams] = getInputParamFileName(args)
    for {
      paramsCLP: CmdLineParams <- params
      _ <- putStrLn(s"PgResearch running in mode : ${paramsCLP.commandStr}")
      res <-
        if (paramsCLP.commandStr.toLowerCase == "run")
          PgResearchRun.run(params)
        else if (paramsCLP.commandStr.toLowerCase == "comp")
          PgResearchComp.comp(params)
        else
          Task(0)
    } yield res
  }

}