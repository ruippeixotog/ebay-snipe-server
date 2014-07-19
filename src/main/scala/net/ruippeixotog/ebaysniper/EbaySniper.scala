package net.ruippeixotog.ebaysniper

import com.jbidwatcher.auction.server.ebay.ebayServer
import com.jbidwatcher.util.Currency
import com.jbidwatcher.util.db.ActiveRecord

object EbaySniper extends App {
  ActiveRecord.disableDatabase()

  val password = "<password>"
  val server = new ebayServer("ebay.com", "ruipeixotog", password)
  val auctionId = "121388541004"

  val auction = server.create(auctionId)
  val currency = auction.getDefaultCurrency

  println(auction)
  println("-- " + auction.getTitle + " --")
  println("  Seller - " + auction.getSellerName)
  println("  Min bid - " + auction.getMinBid)
  println()

  val toBid = Currency.getCurrency(currency.getCurrencySymbol, 0.02)
  println("To bid: " + toBid + " (" + toBid.fullCurrencyName() + ")")

//  val res = server.bid(auctionId, toBid, 1)
//  println(res)
}
