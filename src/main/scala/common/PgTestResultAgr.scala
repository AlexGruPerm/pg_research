package common

/**
 *  Preset common tests result and individual tests results as  sqPgTestResult
*/
case class PgTestResultAgr(sqPgTestResult :Seq[PgTestResult]){

  def sumExecDur :Long = sqPgTestResult.map(tr => tr.durExecMs).sum
  def sumFetchDur :Long = sqPgTestResult.map(tr => tr.durFetchMs).sum
  def sumTotalDur :Long = sqPgTestResult.map(tr => tr.durTotalMs).sum

  def avgExecDur(testNum :Int) :Double = {
    val sq = sqPgTestResult.collect{case tr if tr.loadConf.testNum==testNum => tr.durExecMs}
    sq.sum/sq.size
  }

  override def toString: String =
    s" TotalDuration (e,f,t) = ($sumExecDur  $sumFetchDur  $sumTotalDur) "

  def getAgrStats :Unit =
    sqPgTestResult.map(tr => tr.loadConf.testNum).distinct.map(tnum => (tnum,avgExecDur(tnum))).foreach(println)

}
