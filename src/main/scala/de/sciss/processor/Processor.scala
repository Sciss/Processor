/*
 *  Processor.scala
 *  (Processor)
 *
 *  Copyright (c) 2013-2014 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.processor

import concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}
import de.sciss.model.Model

object Processor {
  /** Processors can throw this exception to signalize abortion. */
  final case class Aborted() extends RuntimeException

  sealed trait Update[+Product, +Repr] { def processor: Repr }

  /** An update indicating the process has progressed.
    *
    * @param processor  the processor that dispatched the update
    * @param amount     the current progression between 0.0 and 1.0
    */
  final case class Progress[Product, Repr](processor: Repr, amount: Double)
    extends Update[Nothing, Repr] {

    override def toString = s"$productPrefix($toInt%)"

    /** Returns an integer progress percentage between 0 and 100 */
    def toInt = (amount * 100).toInt
  }

  /** An update indicating that the process has terminated.
    *
    * @param processor  the processor that dispatched the update
    * @param value      the result which is either a `Success` or a `Failure`
    */
  final case class Result[Product, Repr](processor: Repr, value: Try[Product])
    extends Update[Product, Repr] {

    /** Returns `true` if the process was aborted, otherwise false */
    def isAborted: Boolean = value == Failure(Aborted())
  }

  trait Prepared {
    def start()(implicit exec: ExecutionContext): Unit
  }

  def apply[A](name: => String)(fun: GenericProcessor[A] => A): GenericProcessor[A] = {
    val p = new impl.ProcessorImpl[A, GenericProcessor[A]] with GenericProcessor[A] {
      protected def body(): A = fun(this)
      override def toString = name

      start()
    }
    p
  }
}
/** A processor extends a `Future` with the possibility of being
  * observed and use-site induced abortion. */
trait Processor[+Product, Repr]
  extends Future[Product] with Model[Processor.Update[Product, Repr]] {
  /** Asynchronously aborts the process. This method returns immediately. Once the process is aborted,
    * it will dispatch an `Aborted` event. This method may be called repeatedly, although calling
    * it twice does not have any particular effect.
    */
  def abort(): Unit

  /** Queries the correct progress which is value between 0.0 and 1.0 */
  def progress: Double
}

trait GenericProcessor[Product] extends Processor[Product, GenericProcessor[Product]]