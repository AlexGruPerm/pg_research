package testexec

import common._
import dbconn.PgConnection
import zio.Task
import scala.language.postfixOps

/*
import common._
import dbconn.PgConnection
import zio.Task
import scala.language.postfixOps
*/

object PgRunCasesExec {
  /**
   * Global enter point for running iterations of tests.
   *
   * For global repeat
   *   (Task.foreach(List(1,2,3))(extIter => xxxExec())).map(_.flatten)
   *
  */
  val run : (runAsType , PgRunProp, PgConnectProp, Seq[PgLoadConf]) => Task[Seq[PgTestResult]] =
  (runAs, runProperties, dbConProps, sqLoadConf) =>
    runAs match {
    case _ :runAsSeq.type    => seqExec(runProperties,dbConProps,sqLoadConf)
    case _ :runAsSeqPar.type => seqParExec(runProperties,dbConProps,sqLoadConf)
    case _ :runAsParSeq.type => parSeqExec(runProperties,dbConProps,sqLoadConf)
    case _ :runAsParPar.type => parParExec(runProperties,dbConProps,sqLoadConf)
  }

   private val seqExec: (PgRunProp, PgConnectProp, Seq[PgLoadConf]) => Task[Seq[PgTestResult]] =
    (runProperties, dbConProps, sqLoadConf) =>
      (new PgConnection).sess(0, dbConProps).flatMap { thisSess =>
          Task.foreach(1 to runProperties.repeat reverse) {
            iteration =>
              Task.foreach(sqLoadConf.sortBy(_.testNum).reverse) {
                lc => PgTestExecuter.exec(iteration, thisSess, lc)
              }
          }.map(_.flatten)
      }

  private val parParExec: (PgRunProp, PgConnectProp, Seq[PgLoadConf]) => Task[Seq[PgTestResult]] =
    (runProperties, dbConProps, sqLoadConf) =>
      Task.foreachPar((1 to runProperties.repeat).toList.flatMap(
        iteration =>
          scala.util.Random.shuffle(sqLoadConf).map(lc => (iteration, lc))
      )) { thisIterationLc =>
          (new PgConnection).sess(thisIterationLc._1, dbConProps).flatMap(
          thisSess =>
            PgTestExecuter.exec(thisIterationLc._1, thisSess, thisIterationLc._2)
        )
      }

  private val parSeqExec: (PgRunProp, PgConnectProp, Seq[PgLoadConf]) => Task[Seq[PgTestResult]] =
    (runProperties, dbConProps, sqLoadConf) =>
      Task.foreachPar(1 to runProperties.repeat){
        iteration => (new PgConnection).sess(iteration,dbConProps).flatMap(thisSess =>
          Task.foreach(sqLoadConf.sortBy(_.testNum).reverse){lc =>
            PgTestExecuter.exec(iteration, thisSess, lc)}
        )
      }.map(_.flatten)

  private val seqParExec: (PgRunProp, PgConnectProp, Seq[PgLoadConf]) => Task[Seq[PgTestResult]] =
    (runProperties, dbConProps, sqLoadConf) =>
      Task.foreach(1 to runProperties.repeat) {
        iteration =>
          Task.foreachPar(sqLoadConf)(lc => (new PgConnection).sess(iteration, dbConProps).flatMap(thisSess =>
              PgTestExecuter.exec(iteration, thisSess, lc))
          )
      }.map(_.flatten)

}
