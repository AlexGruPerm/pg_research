package application
import common._
import dbconn.{PgConnection, pgSess}
import loadconf.PgLoadConfReader
import org.slf4j.LoggerFactory
import saveresults.PgSaveResultAsJson
import testexec.PgTestExecuter
import java.util.concurrent.TimeUnit

import zio.clock.Clock
import zio.console._
import zio.{Task, _}



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
        s.sqPgTestResult.foreach(println);
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
   *  MONITORING SESSIONS IN POSTGRES:
   *
   * select pid as process_id,
   * usename as username,
   * datname as database_name,
   * client_addr as client_address,
   * application_name,
   * backend_start,
   * state,
   * state_change
   * from pg_stat_activity p
   * where coalesce(p.usename,'-')='prm_salary'
   *
  */

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

  private val PgResearchLive : List[String] => ZIO[Console with Clock, Throwable, PgTestResultAgr] =
    args => for {
      fileName <- getInputParamFileName(args)
      _ <- putStrLn(s"Begin with config file $fileName")
      dbConProps :PgConnectProp <- PgLoadConfReader.getDbConnectionProps(fileName)
      sqLoadConf :Seq[PgLoadConf] <- PgLoadConfReader.getLoadItems(fileName)
      pgSess :pgSess <- (new PgConnection).sess(dbConProps)
      sess :java.sql.Connection = pgSess.sess
      _ <- putStrLn(s"Connection opened - ${!sess.isClosed}")
      runProperties :PgRunProp <- PgLoadConfReader.getPgRunProp(fileName)
      _ <- putStrLn(s"Running in mode - ${runProperties.runAs} iterations = ${runProperties.repeat}")
      tBegin <- clock.currentTime(TimeUnit.MILLISECONDS)
      sqTestResults :Seq[PgTestResult] <-
        if (runProperties.runAs == "seq") {
          //:todo remove val r and return t
         val t : Task[Seq[PgTestResult]] =
           IO.sequence((1 to runProperties.repeat)
             .flatMap(iterNum => sqLoadConf.map(lc => PgTestExecuter.exec(pgSess, lc))))
          t
        }
        else if (runProperties.runAs == "seqpar") {
         val tskListListPgRes : Task[List[List[PgTestResult]]] =
           IO.sequence((1 to runProperties.repeat).map(
           iterNum =>  {
             val  ri :Task[List[PgTestResult]] = {
                for {
                   sessPar :pgSess <- (new PgConnection).sess(dbConProps)
                   lst :List[PgTestResult] <- ZIO.collectAllPar(
                   sqLoadConf.map(lc => PgTestExecuter.exec(sessPar,lc))
                 )
               } yield lst
             }
             ri
           }
         ))
          //Task[List[PgTestResult]]
            for {
              ll: List[List[PgTestResult]] <- tskListListPgRes.map(ll => ll)
              l <- Task(ll.flatten)
            } yield l
        }
        else {//parallel with degree tests count * repeat
          for {
            //todo: check that it's real parallel execution.
            //sessPar :pgSess <- (new PgConnection).sess(dbConProps)
            joinedFibers <- ZIO.collectAllPar(
              (1 to runProperties.repeat)
                .flatMap(iterNum => sqLoadConf.map(
                  lc =>
                  for {
                    thisSess <- (new PgConnection).sess(dbConProps)
                    tr <- PgTestExecuter.exec(thisSess, lc)
                  } yield tr
                )
                )
            )
          } yield joinedFibers
        }
      tEnd <- clock.currentTime(TimeUnit.MILLISECONDS)
      testAgrResult :PgTestResultAgr = PgTestResultAgr(sqTestResults,tEnd-tBegin)
      _/*saveOutputStatus*/ <- PgSaveResultAsJson.saveResIntoFile(testAgrResult)
    } yield testAgrResult

}
/**
 *
 *    scala.util.Random.shuffle(seqTickers)
 *
*/
