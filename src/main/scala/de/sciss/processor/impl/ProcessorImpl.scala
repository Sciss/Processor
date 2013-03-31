/*
 *  ProcessorImpl.scala
 *  (Processor)
 *
 *  Copyright (c) 2013 Hanns Holger Rutz. All rights reserved.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.processor
package impl

import concurrent.{ExecutionContext, Future, Promise, future}
import de.sciss.model.impl.ModelImpl

/** An implementation of `Pprocessor` along with `Processor.Prepared` that handles
  * everything except the main processing loop. This must be specified as the only
  * abstract method `body`.
  *
  * @tparam Product the result of the process
  * @tparam Repr    the self type of the processor
  */
trait ProcessorImpl[Product, Repr] extends Processor[Product, Repr]
  with Processor.Prepared with ModelImpl[Processor.Update[Product, Repr]] with FutureProxy[Product] {

  self: Repr =>

  private var _context = Option.empty[ExecutionContext]
  @volatile private var _aborted = false
  private var lastProg = -1 // permille resolution

  private val promise = Promise[Product]()

  /** Keeps a record of the execution context used for starting this processor.
    * You may use this to start intermediate sub processes. This method may only
    * be used in the `body` method.
    */
  final implicit protected def exec: ExecutionContext = {
    _context.getOrElse(throw new IllegalStateException("Called before the processor was started"))
  }

  // ---- constructor ----
  final def start()(implicit exec: ExecutionContext) {
    promise.synchronized {
      if (_context.isDefined) return  // was already started
      val res = future { body() }
      res.onComplete(t => dispatch(Processor.Result(self, t)))
      promise.completeWith(res)
      _context = Some(exec)
    }
  }

  final protected def peerFuture: Future[Product] = promise.future

  /**
   * Subclasses may override this to be informed immediately. about an abort request.
   * Otherwise they can pull the aborted status any time by invoking `checkAborted()`.
   */
  protected def notifyAborted() {}

  final def abort() {
    _aborted = true
    notifyAborted()
  }

  /**
   * The main processing body
   */
  protected def body(): Product

  /**
   * Subclasses may override this to perform further cleanup when the process is aborted.
   */
  protected def cleanUp() {}

  /**
   * Checks if the process was aborted. If so, throws an `Aborted` exception. The main body
   * should _not_ try to catch this exception, which will be handled by the underlying infrastructure.
   * However, the main body should but resource operations in proper `try ... finally` blocks, so
   * that these resources are freed when `Abort` exception is thrown. Alternatively, the `cleanUp`
   * method can be overridden to perform such tasks.
   */
  protected final def checkAborted() {
    if (_aborted) throw Processor.Aborted()
  }

  /** The resolution at which progress reports are dispatched. The default of `100` means that
    * a `Processor.Progress` message is only dispatched if the progress has advanced by at least 1 percent.
    * Higher values give finer granularity (sub classes may override this value).
    */
  protected val progressResolution = 100

  /**
   * Invoke this to signalize progress
   *
   * @param f the processor's progress in percent (0 to 1). Values outside the 0 to 1 range will be clipped.
   */
  protected final def progress(f: Float) {
    val f0 = if (f < 0f) 0f else if (f > 1f) 1f else f
    val i = (f0 * progressResolution).toInt
    if (i > lastProg) {
      lastProg = i
      dispatch(Processor.Progress(self, f0))
    }
  }
}