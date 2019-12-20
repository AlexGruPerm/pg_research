package application

/**
 * PgResearch.jar run loadconf.json
 * PgResearch.jar comp file1.json file2.json
*/
case class CmdLineParams(commandStr: String, confFile: Option[String], inputFile1: Option[String], inputFile2: Option[String])
