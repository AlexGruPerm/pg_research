package dbconn

import java.sql.{Connection, DriverManager}

import common.PgConnectProp
import zio._

trait jdbcSession {

  /**
   * Return Connection to Postgres or Exception
  */
  def createPgSess: PgConnectProp => Task[Connection] = (cp) =>
    Task {
      Class.forName(cp.driver)
      DriverManager.getConnection(cp.url, cp.username, cp.password)
    }

}

/**
 *  Singleton object that keep db connection.
*/
object PgConnection extends jdbcSession {

  //todo: read PgConnectProp properties single time from input json.
  val sess : PgConnectProp => Task[Connection] = conProp =>
    createPgSess(conProp/*
    PgConnectProp("org.postgresql.Driver",
      "jdbc:postgresql://172.17.100.53/db_ris_mkrpk",
      "prm_salary",
      "prm_salary"
    )
   */
  )

}

