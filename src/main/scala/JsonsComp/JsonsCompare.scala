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
  val saveResIntoFile : SummaryOfComp => Task[String] = summComp =>
    for {
      fn <- Task(TsToString(System.currentTimeMillis()))
      strExcel <- saveResIntoExcelFile(fn.toString, summComp)
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
  private val saveResIntoExcelFile : (String,SummaryOfComp) => Task[String] = (fn,summComp) => {
    val xlsFileName :String = fn+".xls"

    val wb = new XSSFWorkbook()

    val sheet = wb.createSheet(s"Data")
    val headerRow = sheet.createRow(0)

    headerRow.createCell(0).setCellValue("Description")
    headerRow.createCell(1).setCellValue("1")
    headerRow.createCell(2).setCellValue("2")

    val row1 = sheet.createRow(1)
    row1.createCell(0).setCellValue("File name")
    row1.createCell(1).setCellValue(Paths.get(summComp.f1name).getFileName.toString)
    row1.createCell(2).setCellValue(Paths.get(summComp.f2name).getFileName.toString)

    /*
    val row2 = sheet.createRow(2)
    row2.createCell(0).setCellValue("Mode")
    row2.createCell(1).setCellValue(summComp.f1.map(_.runType).toString)
    row2.createCell(2).setCellValue(summComp.f2.map(_.runType).toString)

    val row3 = sheet.createRow(3)
    row3.createCell(0).setCellValue("Distinct PIDs")
    row3.createCell(1).setCellValue(summComp.f1.map(_.cntDistPids).toString)
    row3.createCell(2).setCellValue(summComp.f2.map(_.cntDistPids).toString)
    */


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
    val r = for {
      sc1 <- summComp.f1
      sc2 <- summComp.f2

      row2 = sheet.createRow(2)
      _ = row2.createCell(0).setCellValue("Mode")
      _ = row2.createCell(1).setCellValue(sc1.runType.toString)
      _ = row2.createCell(2).setCellValue(sc2.runType.toString)

      row3 = sheet.createRow(3)
      _ = row3.createCell(0).setCellValue("Distinct PIDs")
      _ = row3.createCell(1).setCellValue(sc1.cntDistPids.toString)
      _ = row3.createCell(2).setCellValue(sc2.cntDistPids.toString)

    } yield 1
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

    sheet.setDefaultColumnWidth(40)
    val resultFile = new FileOutputStream(xlsFileName)
    wb.write(resultFile)
    resultFile.close

    Task(xlsFileName)
  }

}
