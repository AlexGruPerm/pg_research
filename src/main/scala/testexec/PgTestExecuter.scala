package testexec
import java.sql.{Connection, Types}

import common.{PgLoadConf, PgTestResult}
import org.postgresql.jdbc.PgResultSet
import zio._

object PgTestExecuter {

  val exec :(Connection,PgLoadConf) => Task[PgTestResult] = (con,lc) => {
    /**
    * prm_salary.pkg_web_cons_rep_grbs_list( refcur => 'cursor_unq_name', p_user_id => 45224506);
    */
    val tBegin = System.currentTimeMillis
    con.setAutoCommit(false)

    val procCallText = s"{call ${lc.procName} }"

    val stmt = con.prepareCall(procCallText);

    stmt.setNull(1, Types.OTHER)
    stmt.registerOutParameter(1, Types.OTHER)

    //stmt.setInt(2, 45224506)
    stmt.execute()
    val tExec = System.currentTimeMillis

    // org.postgresql.jdbc.PgResultSet
    val refCur = stmt.getObject(1)

    val pgrs : PgResultSet = refCur.asInstanceOf[PgResultSet]
    val columns: IndexedSeq[(String,String)] = (1 to pgrs.getMetaData.getColumnCount)
      .map(cnum => (pgrs.getMetaData.getColumnName(cnum),pgrs.getMetaData.getColumnTypeName(cnum)))

    val results: Iterator[IndexedSeq[String]] = Iterator.continually(pgrs).takeWhile(_.next()).map{ rs =>
      columns.map(cname => rs.getString(cname._1))
    }
    val rowsCnt = results.size
    val tFetch = System.currentTimeMillis

    Task(PgTestResult(lc,
      cursorColumns = columns,
      durExecMs = tExec - tBegin,
      durFetchMs = tFetch - tExec,
      durTotalMs = tFetch - tBegin,
      numRows = rowsCnt))
  }

}
