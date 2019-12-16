package saveresults

import java.io.{BufferedWriter, File, FileOutputStream, FileWriter}

import common.{PgTestResult, PgTestResultAgr}
import io.circe.syntax._
import io.circe.{Encoder, Printer}
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import zio._

/**
 * Examples: https://circe.github.io/circe/codecs/custom-codecs.html
 * Custom encoders/decoders
*/
object PgSaveResults extends TimestampConverter {

  implicit val encodePgTestResult: Encoder[PgTestResult] =
    Encoder.forProduct12(
      "iterNum",
      "pid","testNum","testName","procName",
      "startTs","endTs","curColumnsSize",
      "durExecMs","durFetchMs","durTotalMs","numRows")(u =>
      (u.iterNum,
        u.pid,
        u.loadConf.testNum,
        u.loadConf.testName,
        u.loadConf.procName,
        u.startTs,
        u.endTs,
        u.cursorColumns.size,
        u.durExecMs,
        u.durFetchMs,
        u.durTotalMs,
        u.numRows
      ))

  implicit val encodePgTestResultAgr: Encoder[PgTestResultAgr] =
    Encoder.forProduct6(
      "cntDistPids","CommonDurMs",
      "sumExecDurMs","sumFetchDurMs", "sumTotalDurMs",
      "test_results")(u =>
      (u.cntDistPids, u.commDurMs,
        u.sumExecDur, u.sumFetchDur, u.sumTotalDur,
        u.sqPgTestResult)
    )

  /**
   *  Save results into output files.
   */
  val saveResIntoFiles : PgTestResultAgr => Task[String] =  sqPgTestResult =>
  //todo: try here foreachPar to save results in parallel.
  {
    for {
      fn <- Task(TsToString(System.currentTimeMillis()))
      strJson <- saveResIntoJsonFile(fn.toString, sqPgTestResult)
      strExcel <- saveResIntoExcelFile(fn.toString, sqPgTestResult)
    } yield strJson+","+strExcel
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
  private val saveResIntoExcelFile : (String,PgTestResultAgr) => Task[String] = (fn,sqPgTestResult) => {
    val xlsFileName :String = fn+".xls"
    val wb = new XSSFWorkbook()
    val sheet = wb.createSheet("Data")
    val headerRow = sheet.createRow(0)
    headerRow.createCell(0).setCellValue("Итерация / Тесты")

    /*
     "iterNum",
      "pid","testNum","testName","procName",
      "startTs","endTs","curColumnsSize",
      "durExecMs","durFetchMs","durTotalMs","numRows"
    */

    val colNames :Seq[String] = Seq("# Итерации", "pid","Номер теста","Имя теста","Имя процедуры",
                                    "Старт теста TS","Окончание теста TS","Кололнок в курсоре",
                                    "Exec мс.","Fetch мс.","Total мс.","Число записей")

    for(hdr <- 1 to colNames.size)
      headerRow.createCell(hdr).setCellValue(hdr)

    for(rowNumber <- (1 to 10)) {
      val row = sheet.createRow(rowNumber)
      row.createCell(0).setCellValue(s"row name $rowNumber")
      for(idx <- 1 to 10) {
        row.createCell(idx).setCellValue(s"$idx _ $rowNumber")
      }
    }

    val resultFile = new FileOutputStream(xlsFileName)
    wb.write(resultFile)
    resultFile.close

    Task(xlsFileName)
  }

  /**
   *  Save results into output json and return it's name.
  */
  private val saveResIntoJsonFile : (String,PgTestResultAgr) => Task[String] = (fn,sqPgTestResult) => {
      val resAsJson = sqPgTestResult.asJson
      val jsonFileName :String = fn+".json"
      writeFile(jsonFileName,Printer.spaces2.pretty(resAsJson))//noSpaces for compaction.
      Task(jsonFileName)
    }

}
