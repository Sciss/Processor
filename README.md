# Processor

## statement

Processor is a simple building block for the Scala programming language, launching asynchronous processing tasks. It is (C)opyright 2013&ndash;2014 by Hanns Holger Rutz. All rights reserved. This project is released under the [GNU Lesser General Public License](https://raw.github.com/Sciss/Processor/master/LICENSE) and comes with absolutely no warranties. To contact the author, send an email to `contact at sciss.de`

## linking

To link to this library:

    libraryDependencies += "de.sciss" %% "processor" % v

The current version `v` is `"0.3.+"`

## building

This project currently builds against Scala 2.10, using sbt 0.13.

## example

A `Processor` is a future which can be observed and aborted. It has two type constructor parameters, the product or payload of the future, and the self or representation type (use `GenericProcessor` if you do not want to provide a particular `Repr`). Processors are typically instantiated through a `ProcessorFactory` which also specifies a `Config` parameter which can be used to configure the processor. An implementation of `ProcessorFactory` needs to provide the `prepare` which returns the processor with a mixin of `Processor.Prepared` (which essentially adds a `start` method).

A convenient trait `ProcessorImpl` managed the whole processor and merely asks for the implementation of the `body` method. The process presented by the processor runs within this method, and should typically call `process` in regular intervals for observations such as GUIs to keep track of the advancement of the process. It should also either override the `notifyAborted` method or make sure to call into `checkAborted` at regular intervals in order to quickly respond to an abortion request.

Here is an example that shows most of the functionality, running two instances of the same processor type in parallel, monitoring their progress, and upon completion of either of them, abort the remaining processor:

```scala

    import de.sciss.processor._
    import concurrent._
    import duration.Duration.Inf

    case class Tea(variety: String)

    object MakeTea extends ProcessorFactory {
      type Product = Tea
      case class Config(variety: String, minutes: Int)
      type Repr = MakeTea

      protected def prepare(config: Config): Prepared = new Impl(config)

      private class Impl(config: Config) extends MakeTea with impl.ProcessorImpl[Tea, MakeTea] {
        override def toString = config.variety

        protected def body(): Tea = blocking {
          val seconds = config.minutes * 60
          for (i <- 1 to seconds) {
            Thread.sleep(1000)
            checkAborted()
            progress = i.toDouble/seconds
          }
          new Tea(config.variety)
        }
      }
    }
    trait MakeTea extends Processor[Tea, MakeTea]

    val black = MakeTea(MakeTea.Config("black", 4))
    val green = MakeTea(MakeTea.Config("green", 3))
    val all   = black :: green :: Nil

    val obs: MakeTea.Observer = {
      case prog @ Processor.Progress(p, _)  => println(s"$p brew ${prog.toInt}%")
      case Processor.Result(_, v)           => println(v)
    }

    all.foreach(_.addListener(obs))

    import ExecutionContext.Implicits.global
    all.foreach(_.start())

    Await.ready(Future.firstCompletedOf(all), Inf)
    all.foreach { p =>
      p.abort() // don't bother about the remaining teas
      Await.ready(p, Inf)
    }
```
