package net.ruippeixotog.ebaysniper.browser

import net.ruippeixotog.ebaysniper.model._

trait BiddingClient {
  def login(): Unit
  def auctionInfo(auctionId: String): Auction
  def bid(auctionId: String, bid: Currency, quantity: Int): Int
}

object BiddingClient {
  import com.jbidwatcher.auction.AuctionServerInterface._

  val isSuccess = Set(BID_WINNING, BID_SELFWIN, BID_BOUGHT_ITEM)

  val statusMessage = Map(
    BID_ERROR_UNKNOWN -> "Unknown reason. Check the auction in the browser to see if the bid went through anyway",
    BID_ERROR_CANNOT -> "The auction cannot be bid on anymore (probably ended)",
    BID_ERROR_AMOUNT -> "Invalid price given",
    BID_ERROR_OUTBID -> "Bid successful but someone else's bid is higher",
    BID_WINNING -> "Bid successful and it is the highest bid for now",
    BID_SELFWIN -> "Bid successful and it is the highest bid for now",
    BID_ERROR_TOO_LOW -> "Bid too low",
    BID_ERROR_ENDED -> "The auction cannot be bid on anymore (probably ended)",
    BID_ERROR_BANNED -> "The user is disallowed from bidding on this seller's items",
    BID_ERROR_RESERVE_NOT_MET -> "Bid successful but below the reserve price",
    BID_ERROR_CONNECTION -> "Connection problem, probably a timeout trying to reach eBay",
    BID_ERROR_TOO_LOW_SELF -> "Bid below or equal to own previous high bid",
    BID_ERROR_AUCTION_GONE -> "The item was removed from JBidwatcher before the bid executed", // should not happen
    BID_ERROR_ACCOUNT_SUSPENDED -> "User's account has been suspended",
    BID_ERROR_CANT_SIGN_IN -> "Sign in failed repeatedly during bid",
    BID_ERROR_WONT_SHIP -> "User is registered in a country to which the seller doesn't ship",
    BID_ERROR_REQUIREMENTS_NOT_MET -> "user doesn't meet some requirement the seller has set for the item. Check the item details for more information",
    BID_ERROR_SELLER_CANT_BID -> "User is the seller of the item").
      withDefaultValue("Unknown status")
}
