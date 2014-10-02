package net.ruippeixotog.ebaysniper.util

import java.util.TimerTask

object Implicits {
  implicit def functionToTimerTask[T](task: => T) = new TimerTask {
    def run() = task
  }

  implicit class RichCloseable[T <: AutoCloseable](val resource: T) extends AnyVal {
    def use[U](block: T => U) = { block(resource); resource.close() }
  }
}
