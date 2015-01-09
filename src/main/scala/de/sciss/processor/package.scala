/*
 *  package.scala
 *  (Processor)
 *
 *  Copyright (c) 2013-2015 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext

package object processor {
  /** Useful extension methods for processors. */
  implicit final class ProcessorOps(val `this`: ProcessorLike[Any, Any]) extends AnyVal { me =>
    import me.{`this` => proc}

    /** A simple progress monitor which prints the current
      * progress to the console, using up to 33 characters.
      */
    def monitor(printResult: Boolean = true)(implicit context: ExecutionContext): Unit = {
      var lastProg = 0
      proc.addListener {
        case prog @ Processor.Progress(_, _) =>
          val p = prog.toInt / 3
          while (lastProg < p) {
            print('#')
            lastProg += 1
          }
      }

      proc.onComplete {
        case Failure(Processor.Aborted()) =>
          println(s" Aborted $proc")

        case Failure(err) =>
          println(s" Failure: $proc")
          err.printStackTrace()

        case Success(res) =>
          println(s" Success: $proc")
          if (printResult) println(s"Result: $res")
      }
    }
  }
}