package application
import java.util.concurrent.TimeUnit

import common._
import dbconn.{PgConnection, pgSess}
import loadconf.PgLoadConfReader
import org.slf4j.LoggerFactory
import saveresults.PgSaveResults
import testexec.PgTestExecuter
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

  //add new types parseq, parpar.
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
   * MONITORING SESSIONS IN POSTGRES:
   * select * from pg_stat_activity p where coalesce(p.usename,'-')='prm_salary'
   * under postgres/postgres
   */


  /**
   * Get application input parameters and return Task[String] with input filename or fail
   * with Exception.
   */
  private val getInputParamFileName: List[String] => Task[String] = argsList =>
    if (argsList.length == 0)
    //todo: don't forget replace succeed with fail.
    //Task.fail(new Exception("No input test config file, use: scala PgResearch <filename.json>"))
      //Task.succeed("/home/gdev/data/home/data/PROJECTS/pg_research/src/main/resources/loadconf.json")
      Task.succeed("C:\\pg_research\\src\\main\\resources\\loadconf.json")
    else
      Task.succeed(argsList(0))


  /**
   * For sequential execution of procedures.
   */
  private val seqExec: (PgRunProp, PgConnectProp, Seq[PgLoadConf]) => Task[Seq[PgTestResult]] =
    (runProperties, dbConProps, sqLoadConf) => {
      for {
        pgSess: pgSess <- (new PgConnection).sess(dbConProps)
        sqTestResults: Seq[PgTestResult] <-
          //IO.sequence(
          ZIO.collectAll(
            (1 to runProperties.repeat).flatMap(
              itn => sqLoadConf.map(lc => PgTestExecuter.exec(itn, pgSess, lc))
            )
          )
      } yield sqTestResults
    }

  /**
   *  For parallel execution of procedures.
   */
  private val parExec : (PgRunProp, PgConnectProp, Seq[PgLoadConf]) => Task[Seq[PgTestResult]] =
    (runProperties, dbConProps, sqLoadConf) =>
      for {
        sqTestResults <- ZIO.collectAllPar(
          (1 to runProperties.repeat).toList.flatMap(i => scala.util.Random.shuffle(sqLoadConf).map(t => (i, t)))
            .map(
              lc =>
                for {
                  thisSess <- (new PgConnection).sess(dbConProps)
                  tr <- PgTestExecuter.exec(lc._1, thisSess, lc._2)
                } yield tr
            )
        )
      } yield sqTestResults

  /**
   *  Run all iterations inparallel with degree = runProperties.repeat
   *  and inside iterations run test sequential
   */
  private val parSeqExec: (PgRunProp, PgConnectProp, Seq[PgLoadConf]) => Task[Seq[PgTestResult]] =
    (runProperties, dbConProps, sqLoadConf) =>
      for {
        sqTestResults <- ZIO.collectAllPar(
          (1 to runProperties.repeat).toList.map(thisIter =>
            for {
              pgSess: pgSess <- (new PgConnection).sess(dbConProps)
              sqTestResults: Seq[PgTestResult] <-
                //IO.sequence(
                 ZIO.collectAll(
                  sqLoadConf.map(lc => PgTestExecuter.exec(thisIter, pgSess, lc))
                )
            } yield sqTestResults
          )
        )
      } yield sqTestResults.flatten

  //todo: remove all xyExec functions in separate file.

  /**
   *  Run all iterations inparallel with degree = runProperties.repeat
   *  and inside iterations run test parallel
   */
  private val parParExec: (PgRunProp, PgConnectProp, Seq[PgLoadConf]) => Task[Seq[PgTestResult]] =
    (runProperties, dbConProps, sqLoadConf) =>
      for {
        sqTestResults <- ZIO.collectAllPar(
          (1 to runProperties.repeat).toList.map(thisIter => execTestsParallel(thisIter, dbConProps, sqLoadConf)
          )
        )
      } yield sqTestResults.flatten



  /**
   * execute test in parallel,
   * for using in seqparExec
   */

  private val execTestsParallel: (Int, PgConnectProp, Seq[PgLoadConf]) => Task[Seq[PgTestResult]] =
    (iterNum, dbConProps, sqLoadConf) =>
      ZIO.collectAllPar(
        sqLoadConf.map( lc => for {
                                  thisSess <- (new PgConnection).sess(dbConProps)
                                  tr :PgTestResult <- PgTestExecuter.exec(iterNum, thisSess, lc)
                                } yield tr
        )
      )



    /*
    OK !!!
  private val execTestsParallel: (Int, PgConnectProp, Seq[PgLoadConf]) => Task[Seq[PgTestResult]] =
    (iterNum, dbConProps, sqLoadConf) =>
      ZIO.collectAllPar(
        sqLoadConf.map( lc =>
          for {
            thisSess <- (new PgConnection).sess(dbConProps)
            tr :PgTestResult <- PgTestExecuter.exec(iterNum, thisSess, lc)
          } yield tr
        )
      )
  */

  /** docs:
   * You can execute two effects in sequence with the flatMap method
   * https://zio.dev/docs/overview/overview_basic_operations
   * Example:
   * val sequenced = getStrLn.flatMap(input => putStrLn(s"You entered: $input"))
   * or
   * val program =
   * for {
   * _    <- putStrLn("Hello! What is your name?")
   * name <- getStrLn
   * _    <- putStrLn(s"Hello, ${name}, welcome to ZIO!")
   * } yield ()
  */


  // IO.sequnce => IO.collectAll
  /**
   * For sequential execution of procedures, inside the iteration procedures execute in parallel.

    //  Task[Seq[PgTestResult]]
    //  execTestsParallel(dbConProps, sqLoadConf))

    Because the ZIO data type supports both flatMap and map, you can use Scala's for comprehensions to build sequential effects:
     for {
    _    <- putStrLn("Hello! What is your name?")
    name <- getStrLn
    _    <- putStrLn(s"Hello, ${name}, welcome to ZIO!")
  } yield ()

   collectAll
   * Evaluate each effect in the structure from left to right, and collect
   * the results.

   */

  private val seqparExec: (PgRunProp, PgConnectProp, Seq[PgLoadConf]) => Task[Seq[PgTestResult]] =
    (runProperties, dbConProps, sqLoadConf) => {

      val xz :Seq[Task[Seq[PgTestResult]]] = (1 to runProperties.repeat).map(thisTaskIter =>
        ZIO.collectAllPar(
          sqLoadConf.map(lc => for {
            thisSess <- (new PgConnection).sess(dbConProps)
            tr: PgTestResult <- PgTestExecuter.exec(thisTaskIter, thisSess, lc)
          } yield tr
          )
        )
      )

      val t :Task[Seq[Seq[PgTestResult]]] = ZIO.collectAll(xz)
      val r :Seq[Task[PgTestResult]] = t
      val res :Task[Seq[PgTestResult]]  = ZIO.collectAll(r) //collectAll convert Seq[Task[... into Task[Seq[...
      res
    }


    /*
      ZIO.collectAll(//remove List( added by yield around return type
      for {
        iters :Task[Int] <- (1 to runProperties.repeat).toList.map(Task(_)) // Seq[Task[Int]] flatMap Task[Int]
        ttListSq :Seq[Task[Seq[PgTestResult]]] = iters.map(thisTaskIter => ZIO.collectAllPar(    // Seq[Task[Int]] map -> Task[Int]  iters.map Int Task[Seq[PgTestResult]]
                                                                                              sqLoadConf.map(lc => for {
                                                                                                thisSess <- (new PgConnection).sess(dbConProps)
                                                                                                tr: PgTestResult <- PgTestExecuter.exec(thisTaskIter, thisSess, lc)
                                                                                               } yield tr
                                                                                              )
                                                                                            )
                    ) // List[Task[Seq[PgTestResult]]]
        tsRes = ZIO.collectAll(ttListSq)
        //tskSeqRes :Task[Seq[PgTestResult]] = ttListSq
       } yield tsRes  // + List( return type )
      )
  */

  /* ok4:

          sqTestResults <- ZIO.collectAll(
            (1 to runProperties.repeat).toList.map(thisIter =>
            ZIO.collectAllPar(
              sqLoadConf.map(lc => for {
                thisSess <- (new PgConnection).sess(dbConProps)
                tr: PgTestResult <- PgTestExecuter.exec(thisIter, thisSess, lc)
              } yield tr
              ))
        ))

  */


/* ok3:

    (runProperties, dbConProps, sqLoadConf) =>
      for {
        sqTestResults: List[Seq[PgTestResult]] <-
          IO.collectAll(
            (1 to runProperties.repeat).map(thisIter => execTestsParallel(thisIter, dbConProps, sqLoadConf))
          )
        r <- Task(sqTestResults.flatten)
      } yield r

*/

  /* ok2:

      (runProperties, dbConProps, sqLoadConf) =>
        for {
             thisIteration <- Task(1 to runProperties.repeat)
               .flatMap(_ => execTestsParallel(dbConProps, sqLoadConf))
        } yield thisIteration

  */

  /* ok:
      (runProperties, dbConProps, sqLoadConf) =>
        for {
          sqTestResults :List[Seq[PgTestResult]] <-
            IO.collectAll(
              (1 to runProperties.repeat)
                .map(
                  _ => execTestsParallel(dbConProps, sqLoadConf)
                )
            )
          r  <- Task(sqTestResults.flatMap(xst => xst))
        } yield r

*/

  /**
   *  Check that connect cridentionals are valid.
  */
  private val checkDbConnectCredits : Task[PgConnectProp] => ZIO[Console, Throwable, Unit] = TdbConProps =>
    for {
      dbConProps <- TdbConProps
      pgSes: pgSess <- (new PgConnection).sess(dbConProps)
      _ <- putStrLn(s"Connection opened - ${!pgSes.sess.isClosed}")
    } yield ()

  /**
   *  Get max_connections from pg config
  */
  private val checkDbMaxConnections : Task[PgConnectProp] => ZIO[Console, Throwable, Unit] = TdbConProps =>
    for {
      dbConProps <- TdbConProps
      settings :PgSettings <- (new PgConnection).getMaxConns(dbConProps)
      //todo: fix problem with reading sourceFile, contains Backslash
      //todo: example: /pgdb/dbfiles/pgsql/11/data/postgresql.conf
      _ <- putStrLn(s"Config : max_connections = ${settings.maxConn} conf : ${settings.sourceFile}")
    } yield ()

  private val PgResearchLive : List[String] => ZIO[Console with Clock, Throwable, PgTestResultAgr] =
    args => for {
      fileName <- getInputParamFileName(args)
      _ <- putStrLn(s"Begin with config file $fileName")
      pgcp = PgLoadConfReader.getDbConnectionProps(fileName)
      dbConProps :PgConnectProp <- pgcp
      sqLoadConf :Seq[PgLoadConf] <- PgLoadConfReader.getLoadItems(fileName)
      _ <- checkDbConnectCredits(pgcp)
      _ <- checkDbMaxConnections(pgcp)
      runProperties :PgRunProp <- PgLoadConfReader.getPgRunProp(fileName)
      _ <- putStrLn(s"Running in mode - ${runProperties.runAs} iterations = ${runProperties.repeat}")
      tBegin <- clock.currentTime(TimeUnit.MILLISECONDS)
      sqTestResults :Seq[PgTestResult] <- runProperties.runAs
      match {
        case _ :runAsSeq.type => seqExec(runProperties,dbConProps,sqLoadConf)
        case _ :runAsSeqPar.type => seqparExec(runProperties,dbConProps,sqLoadConf)
        case _ :runAsPar.type => parExec(runProperties,dbConProps,sqLoadConf)
        case _ :runAsParSeq.type => parSeqExec(runProperties,dbConProps,sqLoadConf)
        case _ :runAsParPar.type => parParExec(runProperties,dbConProps,sqLoadConf)
      }
      tEnd <- clock.currentTime(TimeUnit.MILLISECONDS)
      testAgrResult :PgTestResultAgr = PgTestResultAgr(sqTestResults,tEnd-tBegin)
      saveResStatus <- PgSaveResults.saveResIntoFiles(testAgrResult)
      _ <- putStrLn(s"Results saved in file $saveResStatus")
    } yield testAgrResult


  /*
        fileName <- getInputParamFileName(args)
      _ <- putStrLn(s"Begin with config file $fileName")
      pgcp = PgLoadConfReader.getDbConnectionProps(fileName)
      dbConProps :PgConnectProp <- pgcp
      sqLoadConf :Seq[PgLoadConf] <- PgLoadConfReader.getLoadItems(fileName)
      _ <- checkDbConnectCredits(pgcp)
      runProperties :PgRunProp <- PgLoadConfReader.getPgRunProp(fileName)
      _ <- putStrLn(s"Running in mode - ${runProperties.runAs} iterations = ${runProperties.repeat}")
      tBegin <- clock.currentTime(TimeUnit.MILLISECONDS)
      sqTestResults :Seq[PgTestResult] <- runProperties.runAs
      match {
        case _ :runAsSeq.type => seqExec(runProperties,dbConProps,sqLoadConf)
        case _ :runAsSeqPar.type => seqparExec(runProperties,dbConProps,sqLoadConf)
        case _ :runAsPar.type => parExec(runProperties,dbConProps,sqLoadConf)
      }
      tEnd <- clock.currentTime(TimeUnit.MILLISECONDS)
      testAgrResult :PgTestResultAgr = PgTestResultAgr(sqTestResults,tEnd-tBegin)
      saveResStatus <- PgSaveResults.saveResIntoFiles(testAgrResult)
      _ <- putStrLn(s"Results saved in file $saveResStatus")
*/

}