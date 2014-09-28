package net.ruippeixotog.ebaysniper.model

import com.github.nscala_time.time.Imports._
import com.jbidwatcher.util.Currency

case class Auction(id: String, title: String, endingAt: DateTime, seller: Seller,
                   currentBid: Currency, bidCount: Int, buyNowPrice: Currency, location: String,
                   shippingCost: Currency, thumbnailUrl: String) {

  def ended: Boolean = endingAt.isBeforeNow
  def defaultCurrency: String = Option(currentBid).fold(null: String)(_.fullCurrencyName)
}

object Auction {
  def apply(a: com.jbidwatcher.auction.AuctionInfo): Auction = Auction(
    id = a.getIdentifier,
    title = a.getTitle,
    endingAt = a.getEndDate.toLocalDateTime.toDateTime,
    seller = Seller(a.getSeller),
    currentBid = a.getCurBid,
    bidCount = a.getNumBids,
    buyNowPrice = a.getBuyNow,
    location = a.getItemLocation,
    shippingCost = a.getShipping,
    thumbnailUrl = a.getThumbnailURL)
}
