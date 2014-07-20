package net.ruippeixotog.ebaysniper.util

import java.util.TimerTask

object Implicits {
  implicit def functionToTimerTask[T](task: => T) = new TimerTask {
    def run() = task
  }
}
