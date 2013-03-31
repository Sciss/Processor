/*
 *  ProcessorFactory.scala
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

import concurrent.ExecutionContext
import de.sciss.model.Model

object ProcessorFactory {
  trait WithDefaults extends ProcessorFactory {
    final def apply(): Repr with Processor.Prepared = apply(defaultConfig)
    final def run(observer: Observer)(implicit exec: ExecutionContext): Repr = run(defaultConfig)(observer)

    protected def defaultConfig: Config
  }
}
trait ProcessorFactory {
  // --- abstract stuff ---

  /** Type of the configuration used to instantiate the processor. Use `Unit` if you don't need configuration. */
  type Config

  /** Type of the successful process result. */
  type Product

  /** The actual processor type */
  type Repr

  // --- concrete stuff ---

  final def apply(config: Config): Repr with Processor.Prepared = prepare(config)

  /** Creates and starts the processor.
    *
    * @param config     the configuration to use for the processor
    * @param observer   a partial function observing the progress and completion of the process
    * @param exec       execution context to schedule threads
    * @return           the processor representation
    */
  final def run(config: Config)(observer: Observer)(implicit exec: ExecutionContext): Repr = {
    val p = prepare(config)
    // why the heck do I need this type ascription?
    (p: Model[Processor.Update[Product, Repr]]).addListener(observer)
    p.start()
    p
  }

  /** Must be implemented by sub types. This method is called to instantiate and run the processor.
    *
    * @param config   the configuration as passed into the `apply` method
    * @return         the processor ready to go but not yet started
    */
  protected def prepare(config: Config): Prepared

  type Observer = Model.Listener[Processor.Update[Product, Repr]]

  protected type Generic  = GenericProcessor[Product]
  protected type Prepared = Repr with Processor[Product, Repr] with Processor.Prepared
}