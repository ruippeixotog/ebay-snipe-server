package net.ruippeixotog.ebaysniper

import java.util.{Date, Timer}

import net.ruippeixotog.ebaysniper.browser.BiddingClient
import net.ruippeixotog.ebaysniper.model.Currency
import net.ruippeixotog.ebaysniper.util.Implicits._
import net.ruippeixotog.ebaysniper.util.Logging

import scala.concurrent.{CancellationException, Future, Promise}
import scala.util.Try

case class SnipeInfo(auctionId: String, description: String, bid: Currency,
                     quantity: Int, snipeTime: Option[Date])

class Snipe(val info: SnipeInfo)(implicit ebay: BiddingClient) extends Logging {

  private[this] var timer: Timer = null
  private[this] var promise: Promise[Int] = null

  def activate(): Future[Int] = {
    if(promise != null) promise.future
    else {
      promise = Promise[Int]()
      timer = new Timer(s"${info.auctionId}-snipe", true)

      timer.schedule(promise.complete(Try {
        log.info("Now sniping {}", info)
        ebay.bid(info.auctionId, info.bid, info.quantity)
      }), info.snipeTime.getOrElse(new Date))

      log.info("Scheduled snipe {}", info)
      promise.future
    }
  }

  def cancel(): Unit = if(promise != null) {
    timer.cancel()
    promise.failure(new CancellationException)
    promise = null
    log.info("Cancelled snipe {}", info)
  }
}
