/*
 *  FutureProxy.scala
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
package impl

import concurrent.{CanAwait, ExecutionContext, Future, duration}
import util.Try
import duration.Duration

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
}