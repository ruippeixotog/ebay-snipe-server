package net.ruippeixotog.ebaysniper.model

import com.github.nscala_time.time.Imports._

case class Auction(id: String, title: String, endingAt: DateTime, seller: Seller,
                   currentBid: Currency, bidCount: Int, buyNowPrice: Currency, location: String,
                   shippingCost: Currency, thumbnailUrl: String) {

  def ended: Boolean = endingAt.isBeforeNow
  def defaultCurrency: String = Option(currentBid).fold(null: String)(_.symbol)
}
