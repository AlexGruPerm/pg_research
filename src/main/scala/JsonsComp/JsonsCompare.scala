package JsonsComp

import java.io.{File, FileInputStream}
import java.nio.charset.StandardCharsets

import io.circe.parser._
import zio.clock.Clock
import zio.console.{Console, putStrLn}
import zio.{Task, UIO, ZIO}


/**
 * method for read source json files with results and generate output file
 * with comparison.
*/
object JsonsCompare {

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


  val saveResIntoFiles : SummaryOfComp => Task[String] = summComp =>
  for {
   savedFileName  <- Task("nejopa.json")
  } yield savedFileName


}
