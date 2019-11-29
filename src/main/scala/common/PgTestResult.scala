package common

case class PgTestResult(loadConf :PgLoadConf,
                        cursorColumns: IndexedSeq[(String,String)],
                        durExecMs :Long,
                        durFetchMs :Long,
                        durTotalMs :Long,
                        numRows :Int){
  override def toString =
    s"[${loadConf.testNum}] ${loadConf.testName}   " +
      s"columns = ${cursorColumns.size} " +
      s"rows = $numRows "+
      s"dur =  ($durExecMs  $durFetchMs  $durTotalMs)  ms. "

}
