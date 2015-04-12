package net.ruippeixotog.ebaysniper

import java.io.File

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import net.ruippeixotog.ebaysniper.JsonProtocol._
import net.ruippeixotog.ebaysniper.ebay.EbayClient
import net.ruippeixotog.ebaysniper.util.RoutingLogging
import org.jsoup.HttpStatusException
import spray.http.StatusCodes.{ Success => _, _ }
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import spray.routing.{ ExceptionHandler, SimpleRoutingApp }
import spray.util.LoggingContext

object SnipeServer extends App with SimpleRoutingApp with RoutingLogging with SnipeManagement {

  // load general configuration values
  val config = ConfigFactory.load.getConfig("ebay")

  val username = config.getString("username")
  val password = config.getString("password")
  val site = config.getString("site")

  // create the actor system
  implicit val system = ActorSystem("ebay-sniper")
  implicit def executionContext = system.dispatcher

  // create the eBay server interface
  implicit val ebay = new EbayClient(site, username, password)

  log.info("Logging in into eBay")
  ebay.login()

  override val snipesFile = Option(new File(config.getString("sniper.snipes-file")))
  loadSnipesFromFile()

  def ebayHttpExceptionHandler(implicit log: LoggingContext) = ExceptionHandler {
    case e: HttpStatusException => ctx => {
      log.warning("HTTP {}: {}", e.getStatusCode, e.getUrl)
      e.getStatusCode match {
        case 404 =>
          ctx.complete(NotFound, "The requested auction was not found in eBay's servers.")

        case code if code >= 500 && code <= 599 =>
          ctx.complete(BadGateway, "An error occurred in eBay's servers.")

        case _ =>
          ctx.complete(InternalServerError, "An internal error occurred.")
      }
    }
  }

  // format: OFF
  startServer("0.0.0.0", config.getInt("sniper.port")) {
    handleExceptions(ebayHttpExceptionHandler) {
      logServiceRequest {
        pathPrefix("auction" / Segment) { auctionId =>
          path("snipe") {
            get {
              complete {
                snipes.get(auctionId) match {
                  case None => NotFound -> "No snipe was defined for this auction yet."
                  case Some(snipe) => snipe.info
                }
              }
            } ~
            post {
              entity(as[SnipeInfo]) { reqInfo =>
                complete {
                  scheduler.snipeTimeFor(auctionId, reqInfo.snipeTime) match {
                    case None => BadRequest -> "The auction has already ended."

                    case sTime =>
                      val sInfo = reqInfo.copy(auctionId = auctionId, snipeTime = sTime)
                      registerAndActivate(new Snipe(sInfo))
                      sInfo
                  }
                }
              }
            } ~
            delete {
              complete {
                snipes.get(auctionId) match {
                  case None => NotFound -> "No snipe was defined for this auction yet."
                  case Some(snipe) => snipe.cancel(); snipe.info
                }
              }
            }
          } ~
          pathEnd {
            get { complete(ebay.auctionInfo(auctionId)) }
          }
        } ~
        path("snipes") {
          get { complete(snipes.values.map(_.info)) }
        }
      }
    }
  }
  // format: ON
}
