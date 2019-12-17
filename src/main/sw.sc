

List(1,2,3).zipWithIndex.map(x => println("val = "+ x._1 + " index = " +x._2))


/*
import java.io.{BufferedWriter, File, FileWriter}

import io.circe.{Encoder, _}
import io.circe.syntax._
/*
import java.io.{BufferedWriter, File, FileWriter}
import common.{PgLoadConf, PgTestResult, PgTestResultAgr}
import io.circe.Encoder
import io.circe.syntax._
import io.circe._
import zio._
*/




case class PgLoadConf(testNum :Int, testName :String, procName :String)

implicit val encodePgLoadConf: Encoder[PgLoadConf] =
  Encoder.forProduct1("loadConf")(u =>
    (u.testNum,u.testName,u.procName)
  )

 def writeFile(filename: String, s: String): Unit = {
  val file = new File(filename)
  val bw = new BufferedWriter(new FileWriter(file))
  bw.write(s)
  bw.close()
}

val sr :Seq[PgLoadConf] = Seq(
  PgLoadConf(1,"name1","proc1"),
  PgLoadConf(1,"name2","proc2"),
  PgLoadConf(1,"name3","proc3"))

val resAsJson = sr.asJson
writeFile("c:\\pg_research\\output.json",Printer.noSpaces.pretty(resAsJson))
*/

/*
val resAsJson = List(1, 2, 3).asJson
writeFile("c:\\pg_research\\output.json",Printer.noSpaces.pretty(resAsJson))
*/