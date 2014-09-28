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
        siteConfig.getString("file") + '?' + siteConfig.getString("auction-info.uri-cmd") +
        siteConfig.getString("auction-info.uri-cgi") + auctionId

  def auctionInfo(auctionId: String): Auction = {
    val auctionHtml = browser.get(auctionURL(auctionId))

    val aiConfig = siteConfig.getConfig("auction-info")
    val attrsConfig = aiConfig.getConfig("attributes")
    val contentElems = auctionHtml.select(aiConfig.getString("content-query"))

    def query[T: ClassTag](attr: String): Option[T] =
      contentElems.selectFromConfig(attrsConfig.getConfig(attr)).asInstanceOf[Option[T]]

    val endingAt = query[DateTime]("ending-at").fold[DateTime](new DateTime(0))(
      _.withZone(DateTimeZone.getDefault()))

    val currentBid = query[String]("current-bid").fold[Currency](null)(Currency.getCurrency)
    val buyNowPrice = query[String]("buy-now-price").fold[Currency](null)(Currency.getCurrency)

    val shippingCost = query[String]("shipping-cost") match {
      case None => null
      case Some("FREE") if currentBid == null => null
      case Some("FREE") => Currency.getCurrency(currentBid.getCurrencySymbol, 0.0)
      case Some(price) => Currency.getCurrency(price)
    }

    Auction(auctionId,
      title = query[String]("title").getOrElse(""),
      endingAt = endingAt,
      seller = Seller(
        id = query[String]("seller.id").orNull,
        feedback = query[String]("seller.feedback").fold(0)(_.toInt),
        positivePercentage = query[String]("seller.positive-percentage").fold(100.0)(_.toDouble)),
      currentBid = currentBid,
      bidCount = query[String]("bid-count").fold(0)(_.toInt),
      buyNowPrice = buyNowPrice,
      location = query[String]("location").orNull,
      shippingCost = shippingCost,
      thumbnailUrl = query[String]("thumbnail-url").orNull)
  }

  def bid(auctionId: String, bid: Currency, quantity: Int) = ???
}
