name := "PgResearch"

version := "1.0"

scalaVersion := "2.12.8"

lazy val Versions = new {
  val pgVers = "42.2.5"
  val zioVers = "1.0.0-RC17"
  val circeVers = "0.11.1"
  val poiVers = "4.1.0"
  val dbcp2Vers = "2.7.0"
}

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.typesafeRepo("releases"),
  Resolver.bintrayRepo("websudos", "oss-releases")
)

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe" % "config" % "1.3.4",
  "org.postgresql" % "postgresql" % Versions.pgVers,
  "dev.zio" %% "zio" % Versions.zioVers,
  "org.apache.commons" % "commons-dbcp2" % Versions.dbcp2Vers
)

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
  "io.circe" %% "circe-literal"
).map(_ % Versions.circeVers)

libraryDependencies += "org.apache.poi" % "poi" % Versions.poiVers
libraryDependencies += "org.apache.poi" % "poi-ooxml" % Versions.poiVers

assemblyMergeStrategy in assembly := {
  case x if x.contains("io.netty.versions.properties") => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

assemblyJarName in assembly :="PgResearch.jar"
mainClass in (Compile, packageBin) := Some("application.PgResearch")
mainClass in (Compile, run) := Some("application.PgResearch")

