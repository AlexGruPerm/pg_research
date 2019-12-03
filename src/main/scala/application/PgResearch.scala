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

  //todo: remove all println and use everywhere only _ <- putStrLn
  /**
   * todo: add timout on effects  https://zio.dev/docs/overview/overview_basic_concurrency
   * Timeout
   * ZIO lets you timeout any effect using the ZIO#timeout method,
   *
   */

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
          IO.sequence(
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
   * execute test in parallel,
   * for using in seqparExec
   */
  private val execTestsParallel: (Int, PgConnectProp, Seq[PgLoadConf]) => Task[Seq[PgTestResult]] =
    (iterNum, dbConProps, sqLoadConf) =>
      ZIO.collectAllPar( //Collects from many effects in parallel
        sqLoadConf.map( lc =>
          for {
            thisSess <- (new PgConnection).sess(dbConProps)
            tr :PgTestResult <- PgTestExecuter.exec(iterNum, thisSess, lc)
          } yield tr
        )
      )

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
   */
    //  Task[Seq[PgTestResult]]
    //  execTestsParallel(dbConProps, sqLoadConf))
    /*
    Because the ZIO data type supports both flatMap and map, you can use Scala's for comprehensions to build sequential effects:
     for {
    _    <- putStrLn("Hello! What is your name?")
    name <- getStrLn
    _    <- putStrLn(s"Hello, ${name}, welcome to ZIO!")
  } yield ()
    */

  private val seqparExec: (PgRunProp, PgConnectProp, Seq[PgLoadConf]) => Task[Seq[PgTestResult]] =
    (runProperties, dbConProps, sqLoadConf) =>
      for {
        sqTestResults: List[Seq[PgTestResult]] <-
          IO.collectAll(
            (1 to runProperties.repeat).map(thisIter => execTestsParallel(thisIter, dbConProps, sqLoadConf))
          )
        r <- Task(sqTestResults.flatten)
      } yield r

      /*
      for {
       sqTestResults :Seq[Task[Seq[PgTestResult]]] <- (1 to runProperties.repeat).toList.map(thisIteration => execTestsParallel(thisIteration, dbConProps, sqLoadConf))
       ct :Seq[Seq[PgTestResult]] = ZIO.collectAll(sqTestResults)
      } yield sqTestResults
  */



    /* {
      val lts : Seq[Task[Seq[PgTestResult]]] = (1 to runProperties.repeat).toList.map(itNum => execTestsParallel(itNum, dbConProps, sqLoadConf))
      val ct : Task[Seq[Seq[PgTestResult]]] = ZIO.collectAll(lts)

      val xz :Task[Seq[PgTestResult]] = for {
        ss :Seq[Seq[PgTestResult]] <- ct

        r :Seq[PgTestResult] = ss.flatMap{
          thisGroup =>
            for {
            subGrpRes <- ZIO.collectAllPar(thisGroup)
          } yield subGrpRes
        }

        /*
        r :Seq[PgTestResult] = ss.flatMap{thisGroup =>
          for {
           subGrpRes <- thisGroup
          } yield subGrpRes
        }
        */


        //r  :Seq[PgTestResult] = ss.flatten
      } yield r
*/

      //val seqExecGroups :Task[Seq[PgTestResult]] = Task(ct.flatMap(thisSeq => thisSeq.flatMap(st => st)))
        /*
        for {
        sspr :Seq[PgTestResult] <- seqExecGroups
      } yield sspr
      */
/*
      xz//seqExecGroups
    }
  */


    /*
      for {
        //sqTestResults: Seq[PgTestResult] <-
        itsList :List[Int] <- Task(List(1,2,3,4,5))
        v :List[Task[Seq[PgTestResult]]] <- itsList.flatMap(itNum => execTestsParallel(itNum, dbConProps, sqLoadConf))

        /*
            Task(List(1,2,3,4,5)/*1 to runProperties.repeat*/).flatMap(
              itn => itn.map(itNum => execTestsParallel(itNum, dbConProps, sqLoadConf))
            )
        */

        r <- IO.collectAll(sqTestResults)
      } yield ???//sqTestResults
  */

      /*
      for {
        thisIteration <- (1 to runProperties.repeat).toList.flatMap(iterNum => execTestsParallel(iterNum, dbConProps, sqLoadConf))

        r = v.map(thisList =>
           Task(for {
             ri <- thisList
           } yield ri)
        )
      } yield ??? //thisIteration
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


  private val PgResearchLive : List[String] => ZIO[Console with Clock, Throwable, PgTestResultAgr] =
    args => for {
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