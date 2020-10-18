/*
 *  Processor.scala
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

import de.sciss.model.Model

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

object Processor {
  /** Processors can throw this exception to signalize abortion. */
  final case class Aborted() extends RuntimeException {
    override def toString: String = productPrefix   // not generated by case-class because defined in `Throwable`
  }

  sealed trait Update[+Prod, +Repr] { def processor: Repr }

  /** An update indicating the process has progressed.
    *
    * @param processor  the processor that dispatched the update
    * @param amount     the current progression between 0.0 and 1.0
    */
  final case class Progress[Prod, Repr](processor: Repr, amount: Double)
    extends Update[Nothing, Repr] {

    override def toString = s"$productPrefix($toInt%)"

    /** Returns an integer progress percentage between 0 and 100 */
    def toInt: Int = (amount * 100).toInt
  }

  /** An update indicating that the process has terminated.
    *
    * @param processor  the processor that dispatched the update
    * @param value      the result which is either a `Success` or a `Failure`
    */
  final case class Result[Prod, Repr](processor: Repr, value: Try[Prod])
    extends Update[Prod, Repr] {

    /** Returns `true` if the process was aborted, otherwise false */
    def isAborted: Boolean = value == Failure(Aborted())
  }

  trait Prepared {
    def start()(implicit exec: ExecutionContext): Unit
  }

  trait Body {
    def aborted: Boolean
    def checkAborted(): Unit
    var progress: Double
  }

  /** Creates an ad-hoc processor from a giving body function. The function is
    * passed the resulting processor and should make use of `checkAborted`
    * and `progress`.
    *
    * @param  name  the name is purely informative and will be used for the processor's `toString` method
    */
  def apply[A](name: => String)(fun: Processor[A] with Body => A)(implicit exec: ExecutionContext): Processor[A] = {
    val p = new impl.ProcessorImpl[A, Processor[A]] with Processor[A] {
      protected def body(): A = fun(this)
      override def toString: String = name
    }
    p.start()
    p
  }

  /** Wraps an existing future in the `Processor` interface. The returned
    * processor will not be able to react to `abort` calls, and the `progress`
    * will be reported as zero until the future completes.
    *
    * @param  name  the name is purely informative and will be used for the processor's `toString` method
    */
  def fromFuture[A](name: String, future: Future[A])(implicit exec: ExecutionContext): Processor[A] =
    new impl.FutureWrapper[A](name, future)

  /** Wraps an existing shell process in the `Processor` interface. The returned
    * processor will evaluate to the process' exit value. Calling `abort` will
    * destroy the process.
    *
    * @param  name  the name is purely informative and will be used for the processor's `toString` method
    */
  def fromProcess(name: String, process: scala.sys.process.Process)
                 (implicit exec: ExecutionContext): Processor[Int] = {
    val p = new impl.ProcessWrapper(name, process)
    p.start()
    p
  }
}
/** A processor extends a `Future` with the possibility of being
  * observed and use-site induced abortion. */
trait ProcessorLike[+Prod, +Repr]
  extends Future[Prod] with Model[Processor.Update[Prod, Repr]] {
  /** Asynchronously aborts the process. This method returns immediately. Once the process is aborted,
    * it will dispatch an `Aborted` event. This method may be called repeatedly, although calling
    * it twice does not have any particular effect.
    */
  def abort(): Unit

  /** Queries the correct progress which is value between 0.0 and 1.0 */
  def progress: Double
}

trait Processor[+Prod] extends ProcessorLike[Prod, Processor[Prod]]