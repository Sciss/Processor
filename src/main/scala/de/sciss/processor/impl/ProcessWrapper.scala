/*
 *  ProcessWrapper.scala
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

import scala.concurrent.blocking

class ProcessWrapper(name: String, process: scala.sys.process.Process)
  extends ProcessorImpl[Int, Processor[Int]] with Processor[Int] {

  override def toString = s"$name: $process"

  override protected def notifyAborted(): Unit = process.destroy()

  protected def body(): Int = {
    val res = blocking(process.exitValue())
    checkAborted()
    res
  }
}
