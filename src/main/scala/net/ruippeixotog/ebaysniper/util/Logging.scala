package net.ruippeixotog.ebaysniper.util

import org.slf4j.LoggerFactory
import spray.routing.{Directive0, HttpService}

trait Logging {
  lazy val log = LoggerFactory.getLogger(getClass.getName)
}

trait RoutingLogging extends Logging { this: HttpService =>
  def logServiceRequest: Directive0 = requestInstance.flatMap { req =>
    log.info("{} {} {}", req.method, req.uri.path, req.entity.asString)
    noop
  }
}
