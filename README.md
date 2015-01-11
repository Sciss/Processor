# Processor

## statement

Processor is a simple building block for the Scala programming language, launching asynchronous processing tasks. It is (C)opyright 2013&ndash;2015 by Hanns Holger Rutz. All rights reserved. This project is released under the [GNU Lesser General Public License](https://raw.github.com/Sciss/Processor/master/LICENSE) and comes with absolutely no warranties. To contact the author, send an email to `contact at sciss.de`

## linking

To link to this library:

    libraryDependencies += "de.sciss" %% "processor" % v

The current version `v` is `"0.4.0"`

## building

This project currently builds against Scala 2.11 and 2.10, using sbt 0.13.

## documentation

A `Processor` is a future which can be observed through the `Model` trait and aborted via an `abort` method. The model dispatches an event when the processor is completed either successfully or via failure, and during the processing progress reports are dispatched as `Processor.Progress` events.

The `Processor` trait takes as type constructor parameter the product or payload of the future. The super trait `ProcessorLike` also takes the self or representation type which allows a more specific type to appear in the processor's model updates.

Simple wrappers for existing `Future` or `Process` instances are provided on the `Processor` companion object:

```scala

    import de.sciss.processor._
    import scala.concurrent._
    import ExecutionContext.Implicits.global

    val p0  = "sleep 100".run()
    val p1  = Processor.fromProcess("sleep", p0)
    p1.monitor()
    println("Sleeping...")
    Thread.sleep(2000)
    p1.abort()
```

The `Processor` object also contains an `apply` method to create a processor from a simple function:

```scala

    import de.sciss.processor._
    import scala.concurrent._
    import ExecutionContext.Implicits.global

    val p = Processor[String]("make-tea") { self =>
      blocking {
        for (i <- 1 to 10) {
          Thread.sleep(1000)
          self.checkAborted()
          self.progress = i * 0.1
        }
      }
      "tea"
    }

    p.monitor()
```

Processors are typically instantiated through a `ProcessorFactory` which also specifies a `Config` parameter which can be used to configure the processor. An implementation of `ProcessorFactory` needs to provide the `prepare` which returns the processor with a mixin of `Processor.Prepared` (which essentially adds a `start` method).

A convenient trait `ProcessorImpl` managed the whole processor and merely asks for the implementation of the `body` method. The process presented by the processor runs within this method, and should typically call `process` in regular intervals for observations such as GUIs to keep track of the advancement of the process. It should also either override the `notifyAborted` method or make sure to call into `checkAborted` at regular intervals in order to quickly respond to an abortion request.

Here is an example that shows most of the functionality, running two instances of the same processor type in parallel, monitoring their progress, and upon completion of either of them, abort the remaining processor:

```scala

    import de.sciss.processor._
    import scala.concurrent._
    import duration.Duration.Inf

    case class Tea(variety: String)

    object MakeTea extends ProcessorFactory {
      type Product = Tea
      case class Config(variety: String, minutes: Int)
      type Repr = MakeTea

      protected def prepare(config: Config): Prepared = new Impl(config)

      private class Impl(val config: Config) extends MakeTea with impl.ProcessorImpl[Tea, MakeTea] {
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
    trait MakeTea extends ProcessorLike[Tea, MakeTea] { def config: MakeTea.config }

    val black = MakeTea(MakeTea.Config("black", 4))
    val green = MakeTea(MakeTea.Config("green", 3))
    val all   = black :: green :: Nil

    val obs: MakeTea.Observer = {
      case prog @ Processor.Progress(p, _)  => println(s"${p.config.variety} brew ${prog.toInt}%")
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
