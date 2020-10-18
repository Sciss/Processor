package de.sciss.processor

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, blocking}

object ApplyApp extends App {
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
  Await.ready(p, Duration.Inf)
  Thread.sleep(100)  // allow the monitor to print
}
