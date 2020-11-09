package de.sciss.processor

import de.sciss.processor.Ops._

import scala.concurrent.ExecutionContext.Implicits._
import scala.sys.process._

object ProcessApp extends App {
  val p0  = "sleep 100".run()
  val p1  = Processor.fromProcess("sleep", p0)
  p1.monitor()
  println("Sleeping...")
  Thread.sleep(2000)
  p1.abort()
  Thread.sleep(100) // allow monitor to print
}
