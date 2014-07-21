package net.ruippeixotog.ebaysniper

import java.util.concurrent.CancellationException

import akka.actor.ActorSystem
import com.jbidwatcher.auction.server.ebay.ebayServer
import com.typesafe.config.ConfigFactory
import net.ruippeixotog.ebaysniper.JsonProtocol._
import net.ruippeixotog.ebaysniper.util.RoutingLogging
import spray.http.StatusCodes.{Success => _, _}
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import spray.json._
import spray.routing.SimpleRoutingApp

import scala.util.{Failure, Success}

object SnipeServer extends App with SimpleRoutingApp with RoutingLogging {
  val config = ConfigFactory.load.getConfig("ebay")

  val username = config.getString("username")
  val password = config.getString("password")
  val site = config.getString("site")

  implicit val system = ActorSystem("ebay-sniper")
  import system.dispatcher

  implicit val ebay = new ebayServer(site, username, password)
  ebay.forceLogin()

  var snipes = Map.empty[String, Snipe]
  val scheduler = SnipeScheduler()

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
                    snipes.get(auctionId).foreach(_.cancel())

                    val sInfo = reqInfo.copy(auctionId = auctionId, snipeTime = sTime)
                    val snipe = new Snipe(sInfo)
                    snipes += (auctionId -> snipe)

                    snipe.activate().onComplete { res =>
                      res match {
                        case Success(status) =>
                          log.info("Completed snipe {} with status {}", sInfo, status)

                        case Failure(e: CancellationException) =>
                          log.info("The snipe {} was cancelled", sInfo)

                        case Failure(e) => log.error(s"The snipe $sInfo failed", e)
                      }
                      snipes -= auctionId
                    }

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
          get { complete(ebay.create(auctionId)) }
        }
      }
    }
  }
}
