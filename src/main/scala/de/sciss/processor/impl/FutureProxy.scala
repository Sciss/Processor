/*
 *  FutureProxy.scala
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

import scala.concurrent.duration.Duration
import scala.concurrent.{CanAwait, ExecutionContext, Future, Promise}
import scala.util.control.NonFatal
import scala.util.{Failure, Try}

trait FutureProxy[A] extends Future[A] {
  protected def peerFuture: Future[A]

  def value: Option[Try[A]] = peerFuture.value
  def isCompleted: Boolean  = peerFuture.isCompleted

  def onComplete[U](func: (Try[A]) => U)(implicit executor: ExecutionContext): Unit =
    peerFuture.onComplete(func)

  def ready(atMost: Duration)(implicit permit: CanAwait): this.type = {
    peerFuture.ready(atMost)
    this
  }

  def result(atMost: Duration)(implicit permit: CanAwait): A = peerFuture.result(atMost)

  // ---- new in Scala 2.12 ----
  // to be backwards compatible without branching source,
  // we copy the implementation here

  def transform[B](f: Try[A] => Try[B])(implicit executor: ExecutionContext): Future[B] = {
    val p = Promise[B]()
    onComplete { result => p.complete(try f(result) catch { case NonFatal(t) => Failure(t) }) }
    p.future
  }

  def transformWith[B](f: Try[A] => Future[B])(implicit executor: ExecutionContext): Future[B] = {
    val p = Promise[B]()
    onComplete {
      v => try f(v) match {
        case fut if fut eq this => p complete v.asInstanceOf[Try[B]]
        // Not possible without referring to Scala 2.12 internals:
        // "If possible, link DefaultPromises to avoid space leaks":
        // case dp: DefaultPromise[_] => dp.asInstanceOf[DefaultPromise[B]].linkRootOf(p)
        case fut => p completeWith fut
      } catch { case NonFatal(t) => p failure t }
    }
    p.future
  }
}