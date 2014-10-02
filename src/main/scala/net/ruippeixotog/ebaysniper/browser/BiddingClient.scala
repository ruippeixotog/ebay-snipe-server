package net.ruippeixotog.ebaysniper.browser

import net.ruippeixotog.ebaysniper.model._

trait BiddingClient {
  def login(): Unit
  def auctionInfo(auctionId: String): Auction
  def bid(auctionId: String, bid: Currency, quantity: Int): Int
}
