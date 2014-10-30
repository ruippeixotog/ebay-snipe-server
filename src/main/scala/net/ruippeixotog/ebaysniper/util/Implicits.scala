package net.ruippeixotog.ebaysniper.util

import java.util.TimerTask

import com.typesafe.config.Config

object Implicits {
  implicit def functionToTimerTask[T](task: => T) = new TimerTask {
    def run() = task
  }

  implicit class RichCloseable[T <: AutoCloseable](val resource: T) extends AnyVal {
    def use[U](block: T => U) = { block(resource); resource.close() }
  }

  implicit class RichString(val str: String) extends AnyVal {

    def resolveVars(map: Map[String, String] = Map())(implicit conf: Config): String =
      resolveVars(conf, map)

    def resolveVars(conf: Config, map: Map[String, String]): String = {
      def resolveKey(key: String) = map.getOrElse(key, conf.getString(key))

      "\\{([^\\}]*)\\}".r.findAllIn(str).foldLeft(str) { (curr, key) =>
        curr.replace(key, resolveKey(key.substring(1, key.length - 1)))
      }
    }
  }
}
