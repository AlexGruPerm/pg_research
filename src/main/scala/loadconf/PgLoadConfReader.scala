package loadconf

import zio.{Task, UIO}
import java.io.{File, FileInputStream}
import java.nio.charset.StandardCharsets

import common.{PgConnectProp, PgLoadConf, PgRunProp}

/**
 *
*/
object PgLoadConfReader {

  private def closeStream(is: FileInputStream) =
    UIO(is.close())

  private def readAll(fis: FileInputStream, len: Long): Array[Byte] = {
    val content: Array[Byte] = Array.ofDim(len.toInt)
    fis.read(content)
    content
  }

  private def convertBytes(is: FileInputStream, len: Long) :Task[String] =
    Task.effect(new String(readAll(is, len), StandardCharsets.UTF_8))

  private val confFileContent: String => Task[String] = inputFileName =>
    for {
      file <- Task(new File(inputFileName))
      len = file.length
      string :String <- Task(new FileInputStream(file)).bracket(closeStream)(convertBytes(_, len))
    } yield string

  /**
   *  Read input config file for load tests or research.
   *  And return Seq of instances of type PgLoadConf
  */
  val getLoadItems :String => Task[Seq[PgLoadConf]] = loadConfFileName =>
    for {
      fileStringCont <- confFileContent(loadConfFileName)
      sqPgLoadConf <- PgLoadConfParser.parseConfFileCont(fileStringCont)
    } yield sqPgLoadConf

  /**
   *  Read input config file for load tests or research.
   *  And return db connection properties as instance PgConnectProp
  */
  val getDbConnectionProps :String => Task[PgConnectProp] = loadConfFileName =>
    for {
      fileStringCont <- confFileContent(loadConfFileName)
      connProps <- PgLoadConfParser.parseConnectProps(fileStringCont)
    } yield connProps

  val getPgRunProp :String => Task[PgRunProp] = loadConfFileName =>
    for {
      fileStringCont <- confFileContent(loadConfFileName)
      runProps <- PgLoadConfParser.parseRunProps(fileStringCont)
    } yield runProps

}


