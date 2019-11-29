package loadconf

import common.{PgConnectProp, PgLoadConf, PgRunProp}
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

  implicit val decoder2: Decoder[PgConnectProp] = Decoder.instance { h =>
    for {
      driver <- h.get[String]("driver")
      url <- h.get[String]("url")
      username <- h.get[String]("username")
      password <- h.get[String]("password")
    } yield PgConnectProp(driver,url,username,password)
  }

  implicit val decoder3: Decoder[PgRunProp] = Decoder.instance { h =>
    for {
      runAs <- h.get[String]("runAs")
      repeat <- h.get[Int]("repeat")
    } yield PgRunProp(runAs,repeat)
  }


  //todo: next 2 function look like boilerplate, eliminate it with replacing in one common func.

  /**
   *  get input config file content parse it and return seq of objects that encapsulate
   *  test/research properties. One object for one test.
  */
  val parseConfFileCont : String => Task[Seq[PgLoadConf]] = fileStringCont => {
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

  /**
   *  read connection properties from json, tag = db
  */
  val parseConnectProps : String => Task[PgConnectProp] = fileStringCont => {
    parse(fileStringCont) match {
      case Left (failure) => Task.fail (new Exception (s"Invalid json in input file. $failure"))
      case Right (json) => {
        json.hcursor.downField("db").as[PgConnectProp].swap match {
          case   Left(sq) => Task.succeed(sq)
          case   Right(failure) =>  Task.fail (new Exception (s"Invalid json in input file. $failure"))
        }
      }
    }
  }

  val parseRunProps :String => Task[PgRunProp] = fileStringCont => {
    parse(fileStringCont) match {
      case Left (failure) => Task.fail (new Exception (s"Invalid json in input file. $failure"))
      case Right (json) => {
        json.hcursor.downField("runprops").as[PgRunProp].swap match {
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
