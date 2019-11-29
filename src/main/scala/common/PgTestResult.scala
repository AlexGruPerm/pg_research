package common

/**
 * pid = SELECT pg_backend_pid() as pg_backend_pid
*/
case class PgTestResult( pid : Int,
                         loadConf :PgLoadConf,
                        startTs  :Long,
                        //chronoRunNumber : Int,
                        cursorColumns: IndexedSeq[(String,String)],
                        durExecMs :Long,
                        durFetchMs :Long,
                        durTotalMs :Long,
                        numRows :Int){
  override def toString =
    s"[${loadConf.testNum}] pid=[$pid] startTs [$startTs] " +
      s"${loadConf.testName}   " +
      s"columns = ${cursorColumns.size} " +
      s"rows = $numRows " +
      s"dur =  ($durExecMs  $durFetchMs  $durTotalMs)  ms. "

}
