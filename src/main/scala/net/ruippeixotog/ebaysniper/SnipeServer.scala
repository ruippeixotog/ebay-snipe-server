package net.ruippeixotog.ebaysniper

import java.io.File

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import org.jsoup.HttpStatusException
import spray.json.DefaultJsonProtocol._

import net.ruippeixotog.ebaysniper.JsonProtocol._
import net.ruippeixotog.ebaysniper.ebay.EbayClient
import net.ruippeixotog.ebaysniper.util.RoutingLogging

object SnipeServer extends App with RoutingLogging with SnipeManagement {

  // load general configuration values
  val config = ConfigFactory.load.getConfig("ebay")

  val username = config.getString("username")
  val password = config.getString("password")
  val site = config.getString("site")

  // create the actor system
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit def ec = system.dispatcher

  // create the eBay server interface
  implicit val ebay = new EbayClient(site, username, password)

  log.info("Logging in into eBay")
  ebay.login()

  override val snipesFile = Option(new File(config.getString("sniper.snipes-file")))
  loadSnipesFromFile()

  val ebayHttpExceptionHandler = ExceptionHandler {
    case e: HttpStatusException =>
      log.warn("HTTP {}: {}", e.getStatusCode, e.getUrl)
      e.getStatusCode match {
        case 404 =>
          complete(NotFound, "The requested auction was not found in eBay's servers.")

        case code if code >= 500 && code <= 599 =>
          complete(BadGateway, "An error occurred in eBay's servers.")

        case _ =>
          complete(InternalServerError, "An internal error occurred.")
      }
  }

  // format: OFF
  val serverRoutes = {
    (handleExceptions(ebayHttpExceptionHandler) & logServiceRequest) {
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
  // format: ON

  Http().bindAndHandle(serverRoutes, "0.0.0.0", config.getInt("sniper.port"))
}
