package de.sciss.processor

import scala.concurrent.ExecutionContext
import sys.process._
import ExecutionContext.Implicits._

object ProcessApp extends App {
  val p0  = "sleep 100".run()
  val p1  = Processor.fromProcess("sleep", p0)
  p1.monitor()
  println("Sleeping...")
  Thread.sleep(2000)
  p1.abort()
  Thread.sleep(100) // allow monitor to print
}
