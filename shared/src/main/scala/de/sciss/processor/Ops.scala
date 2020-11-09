/*
 *  Ops.scala
 *  (Processor)
 *
 *  Copyright (c) 2013-2020 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.processor

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object Ops {
  /** Useful extension methods for processors. */
  implicit final class ProcessorOps(private val proc: ProcessorLike[Any, Any]) extends AnyVal { me =>

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