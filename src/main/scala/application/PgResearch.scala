package application
import java.sql.Connection

import common.{PgLoadConf, PgTestResult}
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
    logger.info("PgResearch.run")
    PgResearchLive(List("C:\\pg_research\\src\\main\\resources\\loadconf.json") /*args*/).fold(
      f => {
        logger.error(s"Fail PgResearch.run f=$f msg=${f.getMessage} cause=${f.getCause}")
        f.getStackTrace.foreach(errln => logger.error(errln.toString))
        0
      },
      s => {
        println("Success");
        s.foreach(println)
        1
      }
    )
  }


  /**
   *   Get application input parameters and return Task[String] with input filename or fail
   *   with Exception.
   */
  private val getInputParamFileName : List[String] => Task[String] = argsList =>
    if (argsList.length == 0)
      Task.fail(new Exception("No input test config file, use: scala PgResearch <filename.json>"))
    else
      Task.succeed(argsList(0))

  private val PgResearchLive : List[String] => ZIO[Console, Throwable, Seq[PgTestResult]] =
    args => for {
      fileName <- getInputParamFileName(args)
      _ <- putStrLn(s"Begin with config file $fileName")
      sqLoadConf :Seq[PgLoadConf] <- PgLoadConfReader.getLoadItems(fileName)
      sess :Connection <- PgConnection.sess
      _ <- putStrLn(s"Connection opened - ${!sess.isClosed}")
      sqTestRes :Seq[PgTestResult] <- IO.sequence(sqLoadConf.map(lc => PgTestExecuter.exec(sess,lc)))
      _/*saveOutputStatus*/ <- PgSaveResultAsJson.saveResIntoFile(sqTestRes)
    } yield sqTestRes

}
