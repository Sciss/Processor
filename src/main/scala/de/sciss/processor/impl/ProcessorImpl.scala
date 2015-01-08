/*
 *  ProcessorImpl.scala
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

package de.sciss.processor
package impl

import de.sciss.model.impl.ModelImpl

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future, Promise}

/** An implementation of `Processor` along with `Processor.Prepared` that handles
  * everything except the main processing loop. This must be specified as the only
  * abstract method `body`.
  *
  * @tparam Product the result of the process
  * @tparam Repr    the self type of the processor
  */
trait ProcessorImpl[Product, Repr] extends Processor[Product, Repr]
  with Processor.Prepared with ModelImpl[Processor.Update[Product, Repr]] with FutureProxy[Product] {

  self: Repr =>

  private[this] var _context = Option.empty[ExecutionContext]
  @volatile private[this] var _aborted = false

  @volatile private[this] var _progress = 0.0
  private[this] var lastProg  = -1 // per mille resolution

  private[this] val promise = Promise[Product]()

  /** Keeps a record of the execution context used for starting this processor.
    * You may use this to start intermediate sub processes. This method may only
    * be used in the `body` method.
    */
  final implicit protected def executionContext: ExecutionContext =
    _context.getOrElse(throw new IllegalStateException("Called before the processor was started"))

  // ---- constructor ----
  final def start()(implicit executionContext: ExecutionContext): Unit = promise.synchronized {
    if (_context.isDefined) return  // was already started
    _context = Some(executionContext)
    val res = Future {
      try body() finally cleanUp()
    }
    res.onComplete(t => dispatch(Processor.Result(self, t)))
    promise.completeWith(res)
  }

  final protected def peerFuture: Future[Product] = promise.future

  /** Subclasses may override this to be informed immediately. about an abort request.
    * Otherwise they can pull the aborted status any time by invoking `checkAborted()`.
    */
  protected def notifyAborted(): Unit = ()

  final def abort(): Unit = {
    _aborted = true
    notifyAborted()
  }

  /** The main processing body. */
  protected def body(): Product

  /** Subclasses may override this to perform further cleanup when the process is aborted. */
  protected def cleanUp(): Unit = ()

  /** Checks if the process was aborted. If so, throws an `Aborted` exception. The main body
    * should _not_ try to catch this exception, which will be handled by the underlying infrastructure.
    * However, the main body should put resource operations in proper `try ... finally` blocks, so
    * that these resources are freed when `Abort` exception is thrown. Alternatively, the `cleanUp`
    * method can be overridden to perform such tasks.
    */
  protected final def checkAborted(): Unit = if (_aborted) throw Processor.Aborted()

  protected final def await[B](that: Processor[B, _], offset: Double = 0, weight: Double = 1): B = {
    that.addListener {
      case Processor.Progress(_, p) => progress = p * weight + offset
    }
    val res = Await.result(that, Duration.Inf)
    progress = offset + weight
    res
  }


  /** The resolution at which progress reports are dispatched. The default of `100` means that
    * a `Processor.Progress` message is only dispatched if the progress has advanced by at least 1 percent.
    * Higher values give finer granularity (sub classes may override this value).
    */
  protected val progressResolution = 100

  /** Invoke this to signalize progress
    *
    * @param f the processor's progress in percent (0 to 1). Values outside the 0 to 1 range will be clipped.
    */
  protected final def progress_=(f: Double): Unit = {
    val f0    = if (f < 0.0) 0.0 else if (f > 1.0) 1.0 else f
    _progress = f0
    val i     = (f0 * progressResolution).toInt
    if (i > lastProg) {
      lastProg = i
      dispatch(Processor.Progress(self, f0))
    }
  }

  final def progress: Double = _progress
}