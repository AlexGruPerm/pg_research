package application
import java.sql.Connection

import common.{PgConnectProp, PgLoadConf, PgTestResult, PgTestResultAgr}
import dbconn.PgConnection
import loadconf.PgLoadConfReader
import org.slf4j.LoggerFactory
import saveresults.PgSaveResultAsJson
import testexec.PgTestExecuter
import zio._
import zio.console._

object PgResearch extends App {

  def run(args: List[String]): ZIO[Console, Nothing, Int] = {
    val logger = LoggerFactory.getLogger(getClass.getName)
    val f = PgResearchLive(args).fold(
      f => {
        logger.error(s"Fail PgResearch.run f=$f msg=${f.getMessage} cause=${f.getCause}")
        f.getStackTrace.foreach(errln => logger.error(errln.toString))
        0
      },
      s => {
        println("Success");
        s.sqPgTestResult.foreach(println);
        println("-----------------------------");
        println(s);
        s.getAgrStats
        1
      }
    )
    f
  }


  /**
   *   Get application input parameters and return Task[String] with input filename or fail
   *   with Exception.
   */
  private val getInputParamFileName : List[String] => Task[String] = argsList =>
    if (argsList.length == 0)
      //todo: don't forget replace succeed with fail.
      //Task.fail(new Exception("No input test config file, use: scala PgResearch <filename.json>"))
      Task.succeed("C:\\pg_research\\src\\main\\resources\\loadconf.json")
    else
      Task.succeed(argsList(0))

  private val PgResearchLive : List[String] => ZIO[Console, Throwable, PgTestResultAgr] =
    args => for {
      fileName <- getInputParamFileName(args)
      _ <- putStrLn(s"Begin with config file $fileName")
      dbConProps :PgConnectProp <- PgLoadConfReader.getDbConnectionProps(fileName)
      sqLoadConf :Seq[PgLoadConf] <- PgLoadConfReader.getLoadItems(fileName)
      sess :Connection <- PgConnection.sess(dbConProps)
      _ <- putStrLn(s"Connection opened - ${!sess.isClosed}")
      sqTestResults :Seq[PgTestResult] <- IO.sequence(sqLoadConf.map(lc => PgTestExecuter.exec(sess,lc)))
      testAgrResult :PgTestResultAgr = PgTestResultAgr(sqTestResults)
      _/*saveOutputStatus*/ <- PgSaveResultAsJson.saveResIntoFile(testAgrResult)
    } yield testAgrResult

}
