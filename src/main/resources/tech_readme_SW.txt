/*
import zio._
import zio.console._

case class Res(iterNum :Int, dataValue :Int)

val execUnpure :(Int,Int) => Res = (iterNum,dataValue) =>
 Res(iterNum, dataValue)

val exec :(Int,Int) => Task[Res] = (iterNum,dataValue) =>
  Task.succeed(Res(iterNum, dataValue))

val evalEffectsParallel: (Int, Seq[Int]) => Task[Seq[Res]] =
  (iterNum, sqLoadConf) =>
    ZIO.collectAllPar(
      sqLoadConf.map(lc =>
        for {
          tr: Res <- exec(iterNum, lc)
        } yield tr
      )
    )

val seqParallelExec: (Int, Seq[Int]) => Task[Seq[Res]] =
  (iterNum, sqLoadConf) =>
    for {
      sqTestResults: List[Seq[Res]] <-
      IO.collectAll(
        (1 to iterNum).map(thisIter => evalEffectsParallel(thisIter, sqLoadConf))
      )
      r <- Task(sqTestResults.flatten)
    } yield r

val program : Int => Task[Seq[Res]] = iterationCount =>
      for {
            res <- seqParallelExec(iterationCount, Seq(10,20,30))
        } yield res

(new zio.DefaultRuntime {}).unsafeRun(
  for {
    sp <- program(3)
    _ <- putStrLn(s"seqpar = ${sp.toString}")
  } yield ()
)
*/