package net.ruippeixotog.ebaysniper.browser

import com.jbidwatcher.auction.server.ebay.ebayServer
import net.ruippeixotog.ebaysniper.model._

class JBidwatcherClient(site: String, username: String, password: String) extends BiddingClient {
  private[this] val inner = new ebayServer(site, username, password)

  def login() = inner.forceLogin()
  def auctionInfo(auctionId: String) = Auction(inner.create(auctionId))
  def bid(auctionId: String, bid: Currency, quantity: Int) =
    inner.bid(auctionId, com.jbidwatcher.util.Currency.getCurrency(bid.symbol, bid.value), quantity)
}
