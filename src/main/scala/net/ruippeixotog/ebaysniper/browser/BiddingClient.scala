package net.ruippeixotog.ebaysniper.browser

import com.jbidwatcher.util.Currency
import net.ruippeixotog.ebaysniper.model.Auction

trait BiddingClient {
  def login(): Unit
  def auctionInfo(auctionId: String): Auction
  def bid(auctionId: String, bid: Currency, quantity: Int): Int
}
