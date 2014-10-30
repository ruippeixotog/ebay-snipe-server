package net.ruippeixotog.ebaysniper.browser

import java.io.PrintStream

import com.github.nscala_time.time.Imports._
import com.typesafe.config.ConfigFactory
import net.ruippeixotog.ebaysniper.model._
import net.ruippeixotog.ebaysniper.util.Implicits._
import net.ruippeixotog.ebaysniper.util.Logging
import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.util.Validated._
import org.jsoup.nodes.Document

import scala.reflect.ClassTag

class EbayClient(site: String, username: String, password: String) extends BiddingClient with Logging {
  implicit val browser = new Browser

  implicit val siteConf =
    ConfigFactory.load.getConfig(s"ebay.sites-config.${site.replace('.', '-')}").
      withFallback(ConfigFactory.parseString(s"name = $site"))

  val loginMgr = new EbayLoginManager(siteConf, username, password)

  def login() = loginMgr.forceLogin()

  def auctionInfoUrl(auctionId: String) =
    siteConf.getString("auction-info.uri-template").resolveVars(Map("auctionId" -> auctionId))

  def auctionInfo(auctionId: String): Auction = {
    val attrsConfig = siteConf.getConfig("auction-info.attributes")
    val contentExtractor = Extract.elements(siteConf.getString("auction-info.content-query"))

    val contentHtml = browser.get(auctionInfoUrl(auctionId)) >> contentExtractor

    def query(attr: String): Option[String] =
      contentHtml >?> extractorAt[String](attrsConfig, attr) filter(_.nonEmpty)

    def queryType[T: ClassTag](attr: String): Option[T] =
      contentHtml >?> extractorAt[T](attrsConfig, attr)

    val endingAt = queryType[DateTime]("ending-at").fold(new DateTime(0))(
      _.withZone(DateTimeZone.getDefault()))

    val currentBid = query("current-bid").fold[Currency](null)(Currency.parse)
    val buyNowPrice = query("buy-now-price").fold[Currency](null)(Currency.parse)

    val shippingCost = query("shipping-cost") match {
      case None => null
      case Some("FREE") if currentBid == null => null
      case Some("FREE") => Currency(currentBid.symbol, 0.0)
      case Some(price) => Currency.parse(price)
    }

    Auction(auctionId,
      title = query("title").getOrElse(""),
      endingAt = endingAt,
      seller = Seller(
        id = query("seller.id").orNull,
        feedback = query("seller.feedback").fold(0)(_.toInt),
        positivePercentage = query("seller.positive-percentage").fold(100.0)(_.toDouble)),
      currentBid = currentBid,
      bidCount = query("bid-count").fold(0)(_.toInt),
      buyNowPrice = buyNowPrice,
      location = query("location").orNull,
      shippingCost = shippingCost,
      thumbnailUrl = query("thumbnail-url").orNull)
  }

  def bidFormUrl(auctionId: String, bid: Currency) =
    siteConf.getString("bid-form.uri-template").resolveVars(
      Map("auctionId" -> auctionId, "bidValue" -> bid.value.toString))

  def bid(auctionId: String, bid: Currency, quantity: Int): String = {
    loginMgr.login()
    log.debug("Bidding {} on item {}", bid, auctionId, null)

    def validate(doc: Document, succPath: String, errorsPath: String, desc: String): Option[String] = {
      val succ = matcherAt[String](siteConf, succPath)
      val errors = matchersAt[String](siteConf, errorsPath)

      doc ~/~ (succ, errors, "unknown") match {
        case VSuccess(_) => None

        case VFailure(status) =>
          log.warn("Bid on item {} not successful: {}", auctionId, status, null)

          if(status == "unknown")
            dumpErrorPage(s"$desc-$auctionId-${System.currentTimeMillis()}.html", doc.outerHtml)
          Some(status)
      }
    }

    def processBidFormHtml(bidFormHtml: Document): String = {
      val formExtractor = Extract.formDataAndAction(siteConf.getString("bid-form.form-query"))

      validate(bidFormHtml, "bid-form.success-status", "bid-form.error-statuses", "bid-form") match {
        case Some(status) => status
        case None =>
          val (bidFormData, bidAction) = bidFormHtml >> formExtractor
          val bidConfirmHtml = browser.post(bidAction, bidFormData)
          processBidConfirmHtml(bidConfirmHtml)
      }
    }

    def processBidConfirmHtml(bidConfirmHtml: Document): String = {
      validate(bidConfirmHtml, "bid-confirm.success-status", "bid-confirm.error-statuses", "bid-form") match {
        case Some(status) => status
        case None =>
          log.debug("Successful bid on item {}", auctionId)
          "winning"
      }
    }

    val bidFormHtml = browser.get(bidFormUrl(auctionId, bid))
    processBidFormHtml(bidFormHtml)
  }

  private[this] val pageDumpingEnabled =
    ConfigFactory.load.getBoolean("ebay.debug.dump-unexpected-error-pages")

  private[this] lazy val dumpedPagesDir =
    ConfigFactory.load.getString("ebay.debug.dumped-pages-dir")

  private[this] def dumpErrorPage(filename: String, content: => String) =
    if(pageDumpingEnabled) new PrintStream(s"$dumpedPagesDir/$filename").use(_.println(content))
}
