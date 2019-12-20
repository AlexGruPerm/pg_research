package common

/**
 *  Common properties of test running,
 *  seq, seqpar, par num iterations. -3 possible modes.
 *  For example we have tests 1,2,3
 *  1)
 *  Seq and iterNum = 3 => (1,2,3, 1,2,3, 1,2,3)
 *  2)
 *  seqpar and iterNum = 1 => running in parallel (1,2,3)
 *  seqPar and iterNum = 2 => running in parallel (1,3,2),(2,1,3) random mix. Parallel inside one iteration.
 *  3)
 *   Par (common parallel)
 *  par and iterNum = 1 => running in parallel (1,2,3)
 *  par and iterNum = 3 => running in parallel (1,2,1,3,2,1,3,3,2)
*/
sealed trait runAsType
final case object runAsSeq extends runAsType
final case object runAsSeqPar extends runAsType
final case object runAsParSeq extends runAsType
final case object runAsParPar extends runAsType

case class PgRunProp(runAs :runAsType, repeat :Int){
  def getRunType :String =
    runAs match {
      case _ :runAsSeq.type => "seq"
      case _:runAsSeqPar.type => "seqpar"
      case _:runAsParSeq.type => "parseq"
      case _:runAsParPar.type => "parpar"
      case _ => "nn"
    }
}

object PgRunProp {
  def apply(runAs :String, repeat :Int) : PgRunProp = {
     runAs.toLowerCase match {
       case "seq" => PgRunProp(runAsSeq,repeat)
       case "seqpar" => PgRunProp(runAsSeqPar,repeat)
       case "parseq" => PgRunProp(runAsParSeq,repeat)
       case "parpar" => PgRunProp(runAsParPar,repeat)
       case _ => PgRunProp(runAsSeq,repeat)
     }
  }
}
