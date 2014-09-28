package net.ruippeixotog.ebaysniper.browser

import com.github.nscala_time.time.Imports._
import com.jbidwatcher.util.Currency
import com.typesafe.config.ConfigFactory
import net.ruippeixotog.ebaysniper.browser.Browser._
import net.ruippeixotog.ebaysniper.model.{Auction, Seller}

import scala.reflect.ClassTag

class EbayClient(site: String, username: String, password: String) extends BiddingClient {
  implicit val browser = new Browser

  val siteConfig =
    ConfigFactory.load.getConfig(s"ebay.sites-config.${site.replace('.', '-')}").
      withFallback(ConfigFactory.parseString(s"name = $site"))

  val loginMgr = new EbayLoginManager(siteConfig, username, password)

  def login() = loginMgr.forceLogin()

  def auctionURL(auctionId: String) =
    siteConfig.getString("protocol") + siteConfig.getString("view-host") +
        siteConfig.getString("file") + '?' + siteConfig.getString("view-cmd") +
        siteConfig.getString("view-cgi") + auctionId

  def auctionInfo(auctionId: String): Auction = {
    val auctionHtml = browser.get(auctionURL(auctionId))
    val aiConfig = siteConfig.getConfig("auction-info")

    val strippedElems = auctionHtml.select("head, #LeftSummaryPanel, #RightSummaryPanel")

    def query[T: ClassTag](attr: String): Option[T] =
      strippedElems.selectFromConfig(aiConfig.getConfig(attr)).asInstanceOf[Option[T]]

    val currentBid = Currency.getCurrency(query[String]("current-bid").get)

    val shippingCost = query[String]("shipping-cost") match {
      case None | Some("FREE") => Currency.getCurrency(currentBid.getCurrencySymbol, 0.0)
      case Some(price) => Currency.getCurrency(price)
    }

    val buyNowPrice = query[String]("buy-now-price") match {
      case None => null
      case Some(price) => Currency.getCurrency(price)
    }

    Auction(auctionId,
      title = query[String]("title").get,
      endingAt = query[DateTime]("ending-at").get.withZone(DateTimeZone.getDefault()),
      seller = Seller(
        id = query[String]("seller.id").get,
        feedback = query[String]("seller.feedback").fold(0)(_.toInt),
        positivePercentage = query[String]("seller.positive-percentage").fold(100.0)(_.toDouble)),
      currentBid = currentBid,
      bidCount = query[String]("bid-count").get.toInt,
      buyNowPrice = buyNowPrice,
      location = query[String]("location").get,
      shippingCost = shippingCost,
      thumbnailUrl = query[String]("thumbnail-url").get)
  }

  def bid(auctionId: String, bid: Currency, quantity: Int) = ???
}
