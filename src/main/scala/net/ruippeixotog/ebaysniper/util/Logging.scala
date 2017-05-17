package net.ruippeixotog.ebaysniper.util

import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directives._
import org.slf4j.LoggerFactory

trait Logging {
  lazy val log = LoggerFactory.getLogger(getClass.getName)
}

trait RoutingLogging extends Logging {

  val logServiceRequest: Directive0 = extractRequest.map { req =>
    val entityData = req.entity match {
      case HttpEntity.Strict(_, data) => data.decodeString("UTF-8")
      case _ => "<non-strict>"
    }
    log.info("{} {} {}", req.method.value, req.uri.path, entityData)
    ()
  }
}
