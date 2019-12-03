import zio._
import zio.console._

case class Res(iterNum :Int, v :Int)

val exec :(Int,Int) => Task[Res] = (iterNum,lc) =>
  Task.succeed(Res(iterNum, lc))

/**
  *  Пачка запросов для БД
*/
val si :Seq[Int] = Seq(1,2,3)

val seqExec: (Int, Seq[Int]) => Task[Seq[Res]] =
  (iterCount, sqLoadConf) =>
    for {
      sqTestResults: Seq[Res] <-
      IO.sequence(
        (1 to iterCount).flatMap(
          itn => sqLoadConf.map(lc => exec(itn, lc))
        )
      )
    } yield sqTestResults

val parallelExec: (Int, Seq[Int]) => Task[Seq[Res]] =
  (iterCount, sqLoadConf) =>
    for {
      sqTestResults <- ZIO.collectAllPar(
        (1 to iterCount).toList.flatMap(i => sqLoadConf.map(t => (i, t)))
          .map(
            lc =>
              for {
                tr <- exec(lc._1, lc._2)
              } yield tr
          )
      )
    } yield sqTestResults

val execOneBlockParallel: (Int, Seq[Int]) => Task[Seq[Res]] =
  (iterNum, sqLoadConf) =>
    ZIO.collectAllPar( //Collects from many effects in parallel
      sqLoadConf.map( lc =>
        for {
          tr :Res <- exec(iterNum, lc)
        } yield tr
      )
    )

val seqParallelExec: (Int, Seq[Int]) => Task[Seq[Res]] =
  (iterNum, sqLoadConf) =>
    for {
      sqTestResults: List[Seq[Res]] <-
      IO.collectAll(
        (1 to iterNum).map(thisIter => execOneBlockParallel(thisIter, sqLoadConf))
      )
      r <- Task(sqTestResults.flatten)
    } yield r

/**
  * iterCnt - Количесто итераций, для потоврения N запросов к БД.
  * в режиме seq запросы выполняются в одной сессии БД последовательно, на каждой итерации client session id в бд не меняется.
  * в режиме par делается попытка запустить всё с параллельностью = (все запросы * количесто итераций) - в бд видны все эти
  * разные сессии.
  * в режиме seqpar, итерации идут последовательно, но сами запросы в БД в рамках одной итерации выполняются параллельно.
  * !!! тут как раз вопрос, в бд вижу что в этом режиме все идет как в par, куча сессий сразу.
  *
*/
val program : (String,Int) => Task[Seq[Res]] = (mode,iterCnt) => {
  mode match {
    case "seq" => for {
                    res <- seqExec(iterCnt, si)
                  } yield res
    case "par" => for {
                    res <- parallelExec(iterCnt, si)
                  } yield res
    case "seqpar" => for {
                      res <- seqParallelExec(iterCnt, si)
                    } yield res
    case _ => Task.succeed(Seq())
  }
}

(new zio.DefaultRuntime {}).unsafeRun(
  for {
    p <- program("par",3)
    _ <- putStrLn(s"par = ${p.toString}")
    s <- program("seq",3)
    _ <- putStrLn(s"seq = ${s.toString}")
    sp <- program("seqpar",3)
    _ <- putStrLn(s"seqpar = ${sp.toString}")
  } yield ()
)