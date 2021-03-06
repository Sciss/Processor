/*
 *  ProcessorFactory.scala
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

import scala.concurrent.ExecutionContext

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

  protected type Generic  = Processor[Product]
  protected type Prepared = Repr with ProcessorLike[Product, Repr] with Processor.Prepared
}