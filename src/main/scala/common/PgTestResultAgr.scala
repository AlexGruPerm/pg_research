package common

/**
 *  Preset common tests result and individual tests results as  sqPgTestResult
*/
//commonDurationMs :ZIO[Clock, Nothing, Long]
 case class PgTestResultAgr(sqPgTestResult :Seq[PgTestResult], commDurMs :Long, runProp :PgRunProp){

  def sumExecDur :Long = sqPgTestResult.map(tr => tr.durExecMs).sum
  def sumFetchDur :Long = sqPgTestResult.map(tr => tr.durFetchMs).sum
  def sumTotalDur :Long = sqPgTestResult.map(tr => tr.durTotalMs).sum

  def cntDistPids :Int = sqPgTestResult.map(tr => tr.pid).distinct.size

  def cntDistPidsByTestNum(tn :Int) :Int = sqPgTestResult.filter(tr => tr.loadConf.testNum==tn).distinct.size

  def avgExecDur(testNum :Int) :Double = {
    val sq = sqPgTestResult.collect{case tr if tr.loadConf.testNum==testNum => tr.durExecMs}
    sq.sum/sq.size
  }

  def avgTotalDur(testNum :Int) :Double = {
    val sq = sqPgTestResult.collect{case tr if tr.loadConf.testNum==testNum => tr.durTotalMs}
    sq.sum/sq.size
  }

  def runCount(testNum :Int) :Int =
    sqPgTestResult.collect{case tr if tr.loadConf.testNum==testNum => tr.durExecMs}.size

  override def toString: String = s" Sum of execution time (e,f,t) = ($sumExecDur  $sumFetchDur  $sumTotalDur) "+
  s" PIDS(Distinct cnt)=$cntDistPids"

  def getAgrStats :Unit =
    sqPgTestResult.map(tr => tr.loadConf.testNum).distinct.map(tnum => (tnum,runCount(tnum),avgExecDur(tnum),avgTotalDur(tnum)))
      .foreach{
        ro => println{
          s"[${ro._1}] iterCnt = ${ro._2}  avgExecDur = ${ro._3}  avgTotalDur = ${ro._4} used pids = ${cntDistPidsByTestNum(ro._1)}"
        }
      }

}
