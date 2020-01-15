package JsonsComp

import java.io.{BufferedWriter, File, FileInputStream, FileOutputStream, FileWriter}
import java.nio.charset.StandardCharsets
import java.nio.file.Paths

import io.circe.parser._
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import saveresults.TimestampConverter
import zio.clock.Clock
import zio.console.{Console, putStrLn}
import zio.{Task, UIO, ZIO}


/**
 * method for read source json files with results and generate output file
 * with comparison.
*/
object JsonsCompare extends TimestampConverter {

  private def readAll(fis: FileInputStream, len: Long): Array[Byte] = {
    val content: Array[Byte] = Array.ofDim(len.toInt)
    fis.read(content)
    content
  }

  private def convertBytes(is: FileInputStream, len: Long) :Task[String] =
    Task.effect(new String(readAll(is, len), StandardCharsets.UTF_8))

  private def closeStream(is: FileInputStream) =
    UIO(is.close())

  private val fileContent: String => Task[String] = inputFileName =>
    for {
      file <- Task(new File(inputFileName))
      len = file.length
      string :String <- Task(new FileInputStream(file)).bracket(closeStream)(convertBytes(_, len))
    } yield string


  private val readJsonAsString : String => Task[String] = fileName =>
    for {
      fileContent <- fileContent(fileName)
    } yield fileContent

  val compare: (String, String) => ZIO[Console with Clock,Throwable,SummaryOfComp] = (fileName1, fileName2) =>
    for {
      _ <- putStrLn(s"Reading input json files.")
      file1Content <- readJsonAsString(fileName1)
      file2Content <- readJsonAsString(fileName2)
      _ <- putStrLn(s"Parsing json.")
      j1c <- parse(file1Content) match {
        case Left (failure) => Task.fail (new Exception (s"Invalid json(1) in input file. $failure"))
        case Right (json) => Task.succeed(json)
      }
      j2c <- parse(file2Content) match {
        case Left (failure) => Task.fail (new Exception (s"Invalid json(2) in input file. $failure"))
        case Right (json) => Task.succeed(json)
      }
      resSummaryOfComp <- Task(SummaryOfComp(fileName1, j1c, fileName2, j2c))
    } yield resSummaryOfComp



  /**
   *  Save results into output files.
   */
  val saveResIntoFile: (String, OneFileTests, String, OneFileTests) => Task[String] = (f1name,
                                                                                       f1,
                                                                                       f2name,
                                                                                       f2) => for
    {
      fn <- Task(TsToString(System.currentTimeMillis()))
      strExcel <- saveResIntoExcelFile(fn.toString, f1name, f1, f2name, f2)
    } yield strExcel



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
  private val saveResIntoExcelFile : (String,String, OneFileTests, String, OneFileTests) => Task[String] =
    (fn, f1name, f1, f2name, f2) => {
      val xlsFileName: String = fn + ".xls"
      val wb = new XSSFWorkbook()
      val sheet = wb.createSheet(s"Data")
      val headerRow = sheet.createRow(0)
      headerRow.createCell(0).setCellValue("Description")
      headerRow.createCell(1).setCellValue("1")
      headerRow.createCell(2).setCellValue("2")
      val row1 = sheet.createRow(1)
      row1.createCell(0).setCellValue("File name")
      row1.createCell(1).setCellValue(Paths.get(f1name).getFileName.toString)
      row1.createCell(2).setCellValue(Paths.get(f2name).getFileName.toString)

      val row2 = sheet.createRow(2)
      row2.createCell(0).setCellValue("Mode")
      row2.createCell(1).setCellValue(f1.runType)
      row2.createCell(2).setCellValue(f2.runType)

      val row3 = sheet.createRow(3)
      row3.createCell(0).setCellValue("Distinct PIDs")
      row3.createCell(1).setCellValue(f1.cntDistPids)
      row3.createCell(2).setCellValue(f2.cntDistPids)

      val row4 = sheet.createRow(4)
      row4.createCell(0).setCellValue("CommonDurMs (external coverage tEnd - tBegin)")
      row4.createCell(1).setCellValue(f1.CommonDurMs)
      row4.createCell(2).setCellValue(f2.CommonDurMs)

      val row5 = sheet.createRow(5)
      row5.createCell(0).setCellValue("sumExecDurMs (sum of each call execution)")
      row5.createCell(1).setCellValue(f1.sumExecDurMs)
      row5.createCell(2).setCellValue(f2.sumExecDurMs)

      val row6 = sheet.createRow(6)
      row6.createCell(0).setCellValue("sumFetchDurMs (sum of each call fetching)")
      row6.createCell(1).setCellValue(f1.sumFetchDurMs)
      row6.createCell(2).setCellValue(f2.sumFetchDurMs)

      val row7 = sheet.createRow(7)
      row7.createCell(0).setCellValue("sumTotalDurMs (sum of each call exec + fetch)")
      row7.createCell(1).setCellValue(f1.sumTotalDurMs)
      row7.createCell(2).setCellValue(f2.sumTotalDurMs)

      sheet.setDefaultColumnWidth(50)
      val resultFile = new FileOutputStream(xlsFileName)
      wb.write(resultFile)
      resultFile.close

      Task(xlsFileName)
  }






  /*
    val row2 = sheet.createRow(2)
    row2.createCell(0).setCellValue("Mode")
    val row3 = sheet.createRow(3)
    row3.createCell(0).setCellValue("Distinct PIDs")

    val seqRes = for {
      sc <- Seq(summComp)
      tsc1 = sc.f1
      tsc2 = sc.f2
      _ = row2.createCell(1).setCellValue(tsc1.map(_.runType).toString)
      _ = row2.createCell(2).setCellValue(tsc2.map(_.runType).toString)
    } yield ()

    Task.collectAll(seqRes)//{x => x}.map(_.flatten)
*/




    /*
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
    */


    /*
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
    */
/*
    sheet.setDefaultColumnWidth(40)
    val resultFile = new FileOutputStream(xlsFileName)
    wb.write(resultFile)
    resultFile.close

    Task(xlsFileName)
  }
  */

}
