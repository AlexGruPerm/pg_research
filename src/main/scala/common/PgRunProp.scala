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
case class PgRunProp(runAs :String, repeat :Int)
