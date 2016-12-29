package de.sciss.processor

import impl.ProcessorImpl

object MakeTeaApp extends App {
  import concurrent._
  import duration.Duration.Inf

  case class Tea(variety: String)

  object MakeTea extends ProcessorFactory {
    type Product = Tea
    case class Config(variety: String, minutes: Int)
    type Repr = MakeTea

    protected def prepare(config: Config): Prepared = new Impl(config)

    private class Impl(val config: Config) extends MakeTea with ProcessorImpl[Tea, MakeTea] {
      protected def body(): Tea = blocking {
        val seconds = config.minutes * 60
        for (i <- 1 to seconds) {
          Thread.sleep(1000)
          checkAborted()
          progress = i.toDouble/seconds
        }
        Tea(config.variety)
      }
    }
  }
  trait MakeTea extends ProcessorLike[Tea, MakeTea] {
    def config: MakeTea.Config
  }

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
}