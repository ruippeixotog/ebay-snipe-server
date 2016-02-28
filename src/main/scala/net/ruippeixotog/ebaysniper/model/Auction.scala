package net.ruippeixotog.ebaysniper.model

import com.github.nscala_time.time.Imports._

case class Auction(
    id: String, title: String, endingAt: DateTime, seller: Seller, currentBid: Option[Currency], bidCount: Int,
    buyNowPrice: Option[Currency], location: String, shippingCost: Currency, thumbnailUrl: String) {

  def ended: Boolean = endingAt.isBeforeNow

  def defaultCurrency: String = currentBid.orElse(buyNowPrice) match {
    case None => Currency.Unknown.symbol
    case Some(curr) => curr.symbol
  }
}
