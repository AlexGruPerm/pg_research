package saveresults

import java.io.{BufferedWriter, File, FileOutputStream, FileWriter}

import common.{PgTestResult, PgTestResultAgr}
import io.circe.syntax._
import io.circe.{Encoder, Printer}
import org.apache.poi.ss.usermodel.{FillPatternType, HorizontalAlignment, IndexedColors}
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
   *  https://www.programcreek.com/java-api-examples/?class=org.apache.poi.ss.usermodel.Font&method=setColor
   *  https://www.programcreek.com/java-api-examples/?class=org.apache.poi.ss.usermodel.CellStyle&method=setFillPattern
   *
   */
  private val saveResIntoExcelFile : (String,PgTestResultAgr) => Task[String] = (fn,PgTestResults) => {
    val xlsFileName :String = fn+".xls"
    val wb = new XSSFWorkbook()
    val sheet = wb.createSheet("Data")
    val headerRow = sheet.createRow(0)
    val headerCornerCell = headerRow.createCell(0)

    val cornerStyle = wb.createCellStyle
    val cornerFont = wb.createFont
    cornerFont.setBold(true)
    cornerFont.setFontName("Tahoma")
    cornerFont.setColor(IndexedColors.RED.getIndex)
    cornerFont.setFontHeightInPoints(10)
    cornerStyle.setFont(cornerFont)
    headerCornerCell.setCellStyle(cornerStyle)

    headerCornerCell.setCellValue("Итерация / Тесты")

    val headerStyle = wb.createCellStyle
    val headerFont = wb.createFont
    headerFont.setBold(true)
    headerFont.setFontName("Tahoma")
    headerFont.setFontHeightInPoints(10)
    headerStyle.setFont(headerFont)
    headerStyle.setAlignment(HorizontalAlignment.CENTER)
    headerStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex)
    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND)

    val colNames :Seq[String] = Seq("pid","Номер теста","Имя теста","Имя процедуры",
                                    "Старт теста TS","Окончание теста TS","Кололнок в курсоре",
                                    "Exec мс.","Fetch мс.","Total мс.","Число записей")

    colNames.zipWithIndex.foreach{
      headerNameWithInd =>
        val headerCell = headerRow.createCell(headerNameWithInd._2+1)
        headerCell.setCellValue(headerNameWithInd._1)
        headerCell.setCellStyle(headerStyle)
    }

    /**
     *  Output test results
    */
    val dataStyle = wb.createCellStyle
    dataStyle.setAlignment(HorizontalAlignment.CENTER)
    for (tr <- PgTestResults.sqPgTestResult.sortBy(_.startTs)/*.sortBy(tr => (tr.iterNum,tr.loadConf.testNum))*/.zip(Stream from 1)){
      val PgTestRes :PgTestResult = tr._1
      val RowIndex :Int = tr._2
      val row = sheet.createRow(RowIndex)
      row.createCell(0).setCellValue(PgTestRes.iterNum)
      for (idx <- 1 to 11) {
        val cellVal :String =
          idx match {
            case 1 => PgTestRes.pid.toString
            case 2 => PgTestRes.loadConf.testNum.toString
            case 3 => PgTestRes.loadConf.testName
            case 4 => PgTestRes.loadConf.procName
            case 5 => PgTestRes.startTs.toString
            case 6 => PgTestRes.endTs.toString
            case 7 => PgTestRes.cursorColumns.size.toString
            case 8 => PgTestRes.durExecMs.toString
            case 9 => PgTestRes.durFetchMs.toString
            case 10 => PgTestRes.durTotalMs.toString
            case 11 => PgTestRes.numRows.toString
            case _ => "-"
          }
        val dataCell = row.createCell(idx)
        dataCell.setCellValue(cellVal)
        dataCell.setCellStyle(dataStyle)
        }
      }

    sheet.setDefaultColumnWidth(20)
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
