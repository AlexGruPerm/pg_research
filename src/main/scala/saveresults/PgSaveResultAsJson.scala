package saveresults

import common.PgTestResult
import zio._

object PgSaveResultAsJson {

  val saveResIntoFile : Seq[PgTestResult] => Task[Boolean] =  sqPgTestResult => {
    /*
  defOutputFileName <- Task("output.json")
_ <- putStrLn(s"Test finished, save result into file name (default $defOutputFileName): ")
name <- getStrLn
saveRes <- PgSaveResultAsJson.saveResIntoFile(name,sqTestRes)
  */
    Task(true)
  }

}
