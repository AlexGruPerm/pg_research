package loadconf

import common.PgLoadConf
import io.circe._
import io.circe.parser._
import zio.Task

//https://stackoverflow.com/questions/51800175/parse-json-objects-into-case-classes-in-scala
//https://stackoverflow.com/questions/42288948/how-to-use-circe-for-decoding-json-lists-arrays-in-scala

object PgLoadConfParser {

  implicit val decoder: Decoder[PgLoadConf] = Decoder.instance { h =>
    for {
      testNum <- h.get[Int]("num")
      testName <- h.get[String]("name")
      procName <- h.get[String]("proc")
      /*
      todo: clear
      segments <- h.get[List[String]]("segments")
      ids <- {
        h.getOrElse[List[String]]("seriesIds")(h.get[List[String]]("programmeIds").getOrElse(Nil))
      }
      */
    } yield PgLoadConf(testNum, testName, procName)
  }


  /**
   *  get input config file content parse it and return seq of objects that encapsulate
   *  test/research properties. One object for one test.
  */
  val parseConfFileCont : String => Task[Seq[PgLoadConf]] = fileStringCont => {
    /*
    todo: clear
    val json = parse(fileStringCont).right.get
    val sq = json.hcursor.downField("items").as[Seq[PgLoadConf]]
    */
    parse(fileStringCont) match {
      case Left (failure) => Task.fail (new Exception (s"Invalid json in input file. $failure"))
      case Right (json) => {
        json.hcursor.downField("items").as[Seq[PgLoadConf]].swap match {
          case   Left(sq) => Task.succeed(sq)
          case   Right(failure) =>  Task.fail (new Exception (s"Invalid json in input file. $failure"))
        }
      }
    }
  }


}

/*
todo: clear
decode[PgLoadConf](fileStringCont)
  match {
    case Left (failure) => Task.fail (new Exception ("Invalid json in input file."))
    case Right (json) => {
      Task.succeed(json)
    }
  }
*/
