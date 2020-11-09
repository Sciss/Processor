/*
 *  ProcessorImpl.scala
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
package impl

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/** An implementation of `Processor` along with `Processor.Prepared` that handles
  * everything except the main processing loop. This must be specified as the only
  * abstract method `body`.
  *
  * @tparam Prod the result of the process
  * @tparam Repr    the self type of the processor
  */
trait ProcessorImpl[Prod, Repr] extends ProcessorBase[Prod, Repr] {

  self: Repr =>

  protected def runBody(): Future[Prod] = Future {
    try body() finally cleanUp()
  }

  /** The main processing body. */
  protected def body(): Prod

  /** Subclasses may override this to perform further cleanup when the process is aborted. */
  protected def cleanUp(): Unit = ()

  protected final def await[B](that: ProcessorLike[B, Any], target: Double = 1.0): B = {
    val offset  = progress
    val weight  = target - offset
    val l = that.addListener {
      case Processor.Progress(_, p) => progress = p * weight + offset
    }
    val res = try {
      promise.synchronized { child = that }
      Await.result(that, Duration.Inf)
    } finally {
      promise.synchronized { child = null }
      that.removeListener(l)
    }
    progress = offset + weight
    res
  }
}