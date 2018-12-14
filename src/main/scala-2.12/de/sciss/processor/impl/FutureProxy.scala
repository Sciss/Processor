/*
 *  FutureProxy.scala
 *  (Processor)
 *
 *  Copyright (c) 2013-2017 Hanns Holger Rutz. All rights reserved.
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
import scala.concurrent.{CanAwait, ExecutionContext, Future}
import scala.util.Try

trait FutureProxy[A] extends Future[A] {
  protected def peerFuture: Future[A]

  def value: Option[Try[A]] = peerFuture.value
  def isCompleted: Boolean  = peerFuture.isCompleted

  def onComplete[U](func: Try[A] => U)(implicit executor: ExecutionContext): Unit =
    peerFuture.onComplete(func)

  def ready(atMost: Duration)(implicit permit: CanAwait): this.type = {
    peerFuture.ready(atMost)
    this
  }

  def result(atMost: Duration)(implicit permit: CanAwait): A = peerFuture.result(atMost)

  // ---- new in Scala 2.12 ----

  def transform[B](f: Try[A] => Try[B])(implicit executor: ExecutionContext): Future[B] =
    peerFuture.transform(f)

  def transformWith[B](f: Try[A] => Future[B])(implicit executor: ExecutionContext): Future[B] =
    peerFuture.transformWith(f)
}