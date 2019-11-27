package application
import common.{PgLoadConf, PgTestResult}
import loadconf.PgLoadConfReader
import testexec.PgTestExecuter
import zio._
import zio.console._

object PgResearch extends App {

  def run(args: List[String]): ZIO[Console, Nothing, Int] =
    PgResearchLive(List("C:\\pg_research\\src\\main\\resources\\loadconf.json")/*args*/).fold(
      f => {
        println(s"Fail PgResearch.run f=$f");
        0
      },
      s => {
        println("Success");
        s.foreach(println)
        1
      }
  )


  /**
   *   Get application input parameters and return Task[String] with input filename or fail
   *   with Exception.
   */
  private val getInputParamFileName : List[String] => Task[String] = argsList =>
    if (argsList.length == 0)
      Task.fail(new Exception("No input test config file, use: scala PgResearch <filename.json>"))
    else
      Task.succeed(argsList(0))


  private val saveResultIntoFile : Seq[PgTestResult] => ZIO[Console, Throwable, Boolean] =
    sqTestRes => for {
      /*
        defOutputFileName <- Task("output.json")
      _ <- putStrLn(s"Test finished, save result into file name (default $defOutputFileName): ")
      name <- getStrLn
      saveRes <- PgSaveResultAsJson.saveResIntoFile(name,sqTestRes)
        */
      saveRes <- Task(true)
    } yield saveRes


  private val PgResearchLive : List[String] => ZIO[Console, Throwable, Seq[PgTestResult]] =
    args => for {
      fileName <- getInputParamFileName(args)
      _ <- putStrLn(s"Begin with config file $fileName")
      sqLoadConf :Seq[PgLoadConf] <- PgLoadConfReader.getLoadItems(fileName)
      sqTestRes :Seq[PgTestResult] <- IO.sequence(sqLoadConf.map(PgTestExecuter.exec))
      _/*saveOutputStatus*/ <- saveResultIntoFile(sqTestRes)
    } yield sqTestRes









}
