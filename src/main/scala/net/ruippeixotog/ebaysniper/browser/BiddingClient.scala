package net.ruippeixotog.ebaysniper.browser

import net.ruippeixotog.ebaysniper.model._

trait BiddingClient {
  def login(): Unit
  def auctionInfo(auctionId: String): Auction
  def bid(auctionId: String, bid: Currency, quantity: Int): String
}

object BiddingClient {

  object BidStatus {
    val isSuccess: String => Boolean = Set("winning", "self_win")

    val statusMessage = Map(
      "cannot" -> "The auction cannot be bid on anymore (probably ended)",
      "amount" -> "Invalid price given",
      "outbid" -> "Bid successful but someone else's bid is higher",
      "winning" -> "Bid successful and it is the highest bid for now",
      "self_win" -> "Bid successful and it is the highest bid for now",
      "too_low" -> "Bid too low",
      "ended" -> "The auction cannot be bid on anymore (probably ended)",
      "banned" -> "The user is disallowed from bidding on this seller's items",
      "reserve_not_met" -> "Bid successful but below the reserve price",
      "connection" -> "Connection problem, probably a timeout trying to reach eBay",
      "too_low_self" -> "Bid below or equal to own previous high bid",
      "auction_gone" -> "The item was removed before the bid was executed",
      "account_suspended" -> "User's account has been suspended",
      "cant_sign_in" -> "Sign in failed repeatedly during bid",
      "wont_ship" -> "User is registered in a country to which the seller doesn't ship",
      "requirements_not_met" -> "user doesn't meet some requirement the seller has set for the item. Check the item details for more information",
      "seller_cant_bid" -> "User is the seller of the item").
      withDefaultValue("Unknown reason. Check the auction in the browser to see if the bid went through anyway")
  }
}
