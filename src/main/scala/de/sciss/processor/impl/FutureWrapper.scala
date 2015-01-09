/*
 *  FutureWrapper.scala
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

import scala.concurrent.{ExecutionContext, Future}

class FutureWrapper[A](name: String, protected val peerFuture: Future[A])(implicit exec: ExecutionContext)
  extends Processor[A] with ModelImpl[Processor.Update[A, Processor[A]]] with FutureProxy[A] {

  override def toString = s"$name: $peerFuture"

  def abort(): Unit = ()  // nothing we can do about it

  /** Queries the correct progress which is value between 0.0 and 1.0 */
  def progress: Double = if (peerFuture.isCompleted) 1.0 else 0.0

  peerFuture.onComplete(res => dispatch(Processor.Result(this, res)))
}
