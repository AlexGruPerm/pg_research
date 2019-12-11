
List.range(1, 3 + 1).foreach(println)

(1 to 3).foreach(println)

/*
/**
 * Applies the function `f` to each element of the `Iterable[A]` in parallel,
 * and returns the results in a new `List[B]`.
 *
 */
Task.foreachPar

in Task.scala
final def foreachPar[A, B](as: Iterable[A])(fn: A => Task[B]): Task[List[B]] =
  ZIO.foreachPar(as)(fn)

in zio.Scala
final def foreachPar[R, E, A, B](as: Iterable[A])(fn: A => ZIO[R, E, B]): ZIO[R, E, List[B]] =
  as.foldRight[ZIO[R, E, List[B]]](effectTotal(Nil)) { (a, io) =>
    fn(a).zipWithPar(io)((b, bs) => b :: bs)
  }
    .refailWithTrace

--------------------------------------------------------------

/**
 * Evaluate each effect in the structure in parallel, and collect
 * the results.
 */
Task.collectAllPar

in Task.scala
final def collectAllPar[A](as: Iterable[Task[A]]): Task[List[A]] =
  ZIO.collectAllPar(as)

in ZIO.Scala
final def collectAllPar[R, E, A](as: Iterable[ZIO[R, E, A]]): ZIO[R, E, List[A]] =
  foreachPar[R, E, ZIO[R, E, A], A](as)(ZIO.identityFn)

*/
