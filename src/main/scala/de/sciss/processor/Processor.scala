/*
 *  Processor.scala
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

import concurrent.{ExecutionContext, Future}
import util.Try
import de.sciss.model.Model

object Processor {
  /** Processors can throw this exception to signalize abortion. */
  final case class Aborted() extends RuntimeException

  sealed trait Update[+Product, +Repr] { def processor: Repr }

  final case class Progress[Product, Repr](processor: Repr, percent: Float)
    extends Update[Nothing, Repr] {

    override def toString = s"$productPrefix($toInt%)"

    def toInt = (percent * 100).toInt
  }

  final case class Result[Product, Repr](processor: Repr, value: Try[Product])
    extends Update[Product, Repr]

  trait Prepared {
    def start()(implicit exec: ExecutionContext): Unit
  }
}
/** A processor extends a `Future` with the possibility of being
  * observed and use-site induced abortion. */
trait Processor[Product, Repr]
  extends Future[Product] with Model[Processor.Update[Product, Repr]] {
  /** Asynchronously aborts the process. This method returns immediately. Once the process is aborted,
    * it will dispatch an `Aborted` event. This method may be called repeatedly, although calling
    * it twice does not have any particular effect.
    */
  def abort(): Unit
}

trait GenericProcessor[Product] extends Processor[Product, GenericProcessor[Product]]