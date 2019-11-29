package dbconn

import java.sql.{Connection, DriverManager, ResultSet, Statement}

import common.PgConnectProp
import org.slf4j.LoggerFactory
import zio._

case class pgSess(sess : Connection, pid : Int)

trait jdbcSession {
  val logger = LoggerFactory.getLogger(getClass.getName)

  /**
   * Return Connection to Postgres or Exception
  */
  def createPgSess: PgConnectProp => Task[pgSess] = (cp) =>
    Task {
      Class.forName(cp.driver)
      val c :Connection = DriverManager.getConnection(cp.url, cp.username, cp.password)
      c.setClientInfo("ApplicationName","PgResearch")
      val stmt: Statement = c.createStatement
      val rs: ResultSet = stmt.executeQuery("SELECT pg_backend_pid() as pg_backend_pid")
      rs.next()
      val pg_backend_pid :Int = rs.getInt("pg_backend_pid")
      logger.info(s"User sesison pg_backend_pid = $pg_backend_pid")
      pgSess(c,pg_backend_pid)
    }

}

/**
 *  Singleton object that keep db connection.
*/
object PgConnection extends jdbcSession {

  //todo: read PgConnectProp properties single time from input json.
  val sess : PgConnectProp => Task[pgSess] = conProp =>
    createPgSess(conProp)

}

