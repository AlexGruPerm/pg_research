package saveresults

import java.io.{BufferedWriter, File, FileWriter}

import common.{PgLoadConf, PgTestResult, PgTestResultAgr}
import io.circe.Encoder
import io.circe.syntax._
import zio._

object PgSaveResults {

  implicit val encodePgLoadConf: Encoder[PgLoadConf] =
    Encoder.forProduct1("loadConf")(u =>
      (u.testNum,u.testName,u.procName)
    )

  implicit val encodePgTestResult: Encoder[PgTestResult] =
    Encoder.forProduct1("PgTestResult")(u =>
      (
        u.pid,
        u.loadConf,
        u.startTs,
        u.endTs,
        u.cursorColumns,
        u.durExecMs,
        u.durFetchMs,
        u.durTotalMs,
        u.numRows
      ))

  implicit val encodePgTestResultAgr: Encoder[PgTestResultAgr] =
    Encoder.forProduct1("sqPgTestResult")(u =>
      (u.sqPgTestResult)
    )



  /**
   *  Save results into output files.
   */
  val saveResIntoFiles : PgTestResultAgr => Task[String] =  sqPgTestResult => {
    for {
     str <- saveResIntoJsonFile(sqPgTestResult)
      _ <- console.putStrLn(s" saveResIntoJsonFile res=$str")
    } yield str

    Task("Success saved output files : output_02122019_170005.json, output_02122019_170005.xls")
  }

  private def writeFile(filename: String, s: String): Unit = {
    val file = new File(filename)
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(s)
    bw.close()
  }

  /**
   *  Save results into output Excel and return it's name.
   */
  private val saveResIntoExcelFile : PgTestResultAgr => Task[String] =  sqPgTestResult => {
    ???
    Task("output_02122019_170005.xls")
  }

  /**
   *  Save results into output json and return it's name.
  */
    //todo: make save in ZIO witj bracer/breaker
  private val saveResIntoJsonFile : PgTestResultAgr => Task[String] =  sqPgTestResult => {
    val resAsJson = List(1, 2, 3).asJson//sqPgTestResult.asJson
      println(s"RES0 = $resAsJson")
      println(s"RES1 size = ${sqPgTestResult.sqPgTestResult.size}")
      println(s"RES2 = ${resAsJson.asString}")
      writeFile("output.json",resAsJson.asString.getOrElse("{}"))
      Task( "output.json")
    }


}
