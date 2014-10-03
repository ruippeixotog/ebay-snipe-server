package net.ruippeixotog.ebaysniper

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import net.ruippeixotog.ebaysniper.JsonProtocol._
import net.ruippeixotog.ebaysniper.browser.EbayClient
import net.ruippeixotog.ebaysniper.util.RoutingLogging
import spray.http.StatusCodes.{Success => _, _}
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import spray.routing.SimpleRoutingApp

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
  ebay.login()

  override val snipesFile = Option(config.getString("sniper.snipes-file"))
  loadSnipesFromFile()

  startServer("0.0.0.0", config.getInt("sniper.port")) {
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
