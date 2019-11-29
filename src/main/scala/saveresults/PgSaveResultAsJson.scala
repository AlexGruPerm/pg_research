package saveresults

import common.PgTestResultAgr
import zio._

object PgSaveResultAsJson {

  val saveResIntoFile : PgTestResultAgr => Task[Boolean] =  sqPgTestResult => {
    /*
  defOutputFileName <- Task("output.json")
_ <- putStrLn(s"Test finished, save result into file name (default $defOutputFileName): ")
name <- getStrLn
saveRes <- PgSaveResultAsJson.saveResIntoFile(name,sqTestRes)
  */
    Task(true)
  }

}
