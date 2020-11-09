/*
 *  AbstractProcessor.scala
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
package impl

trait AbstractProcessor     [Prod] extends ProcessorImpl[Prod, Processor[Prod]] with Processor[Prod]
trait AbstractAsyncProcessor[Prod] extends ProcessorBase[Prod, Processor[Prod]] with Processor[Prod]
