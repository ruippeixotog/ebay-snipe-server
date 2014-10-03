package net.ruippeixotog.ebaysniper.browser

import java.io.PrintStream

import com.github.nscala_time.time.Imports._
import com.typesafe.config.ConfigFactory
import net.ruippeixotog.ebaysniper.browser.Browser._
import net.ruippeixotog.ebaysniper.model._
import net.ruippeixotog.ebaysniper.util.Implicits._
import net.ruippeixotog.ebaysniper.util.Logging
import org.jsoup.nodes.Document

import scala.collection.convert.WrapAsScala._
import scala.reflect.ClassTag

class EbayClient(site: String, username: String, password: String) extends BiddingClient with Logging {
  implicit val browser = new Browser

  val siteConfig =
    ConfigFactory.load.getConfig(s"ebay.sites-config.${site.replace('.', '-')}").
      withFallback(ConfigFactory.parseString(s"name = $site"))

  val loginMgr = new EbayLoginManager(siteConfig, username, password)

  def login() = loginMgr.forceLogin()

  def auctionInfoURL(auctionId: String) =
    replaceVars(siteConfig.getString("auction-info.uri-template"), Map("auctionId" -> auctionId))

  def auctionInfo(auctionId: String): Auction = {
    val auctionHtml = browser.get(auctionInfoURL(auctionId))

    val aiConfig = siteConfig.getConfig("auction-info")
    val attrsConfig = aiConfig.getConfig("attributes")
    val contentElems = auctionHtml.select(aiConfig.getString("content-query"))

    def query[T: ClassTag](attr: String): Option[T] =
      contentElems.selectFromConfig(attrsConfig.getConfig(attr)).asInstanceOf[Option[T]]

    val endingAt = query[DateTime]("ending-at").fold[DateTime](new DateTime(0))(
      _.withZone(DateTimeZone.getDefault()))

    val currentBid = query[String]("current-bid").fold[Currency](null)(Currency.parse)
    val buyNowPrice = query[String]("buy-now-price").fold[Currency](null)(Currency.parse)

    val shippingCost = query[String]("shipping-cost") match {
      case None => null
      case Some("FREE") if currentBid == null => null
      case Some("FREE") => Currency(currentBid.symbol, 0.0)
      case Some(price) => Currency.parse(price)
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

  def bidFormURL(auctionId: String, bid: Currency) =
    replaceVars(siteConfig.getString("bid-form.uri-template"),
      Map("auctionId" -> auctionId, "bidValue" -> bid.value.toString))

  def bid(auctionId: String, bid: Currency, quantity: Int): Int = {
    loginMgr.login()
    log.debug("Bidding {} on item {}", bid, auctionId, null)

    def errorStatusCodeFor(doc: Document, errorDefsPath: String, desc: String): Int =
      siteConfig.getConfigList(errorDefsPath).toStream.flatMap { errorDef =>
        doc.selectFromConfig(errorDef.getConfig("select")).asInstanceOf[Option[String]].flatMap { content =>
          errorDef.getString("match").r.findFirstIn(content).map { _ =>
            val status = errorDef.getInt("status")
            log.warn("Bid on item {} not successful: {} (code {})",
              auctionId, BiddingClient.statusMessage(status), status.toString)
            status
          }
        }
      }.headOption.getOrElse {
        log.error("Bid on item {} not successful: {} (code -1)",
          auctionId, BiddingClient.statusMessage(-1), null)
        dumpErrorPage(s"$desc-$auctionId-${System.currentTimeMillis()}.html", doc.outerHtml)
        -1
      }

    def processBidFormHtml(bidFormHtml: Document): Int = {
      val bidForm = bidFormHtml.getElementById("reviewbid")

      if(bidForm == null || bidForm.select("input[name=confirmbid][type=submit]").isEmpty)
        errorStatusCodeFor(bidFormHtml, "bid-form.error-statuses", "bid-form")
      else {
        val bidFormData = bidForm.extractFormData
        val bidConfirmHtml = browser.post(bidForm.attr("action"), bidFormData)
        processBidConfirmHtml(bidConfirmHtml)
      }
    }

    def processBidConfirmHtml(bidConfirmHtml: Document): Int = {
      if(bidConfirmHtml.egrep(siteConfig.getString("bid-confirm.success-message")).isEmpty) {
        errorStatusCodeFor(bidConfirmHtml, "bid-confirm.error-statuses", "bid-confirm")
      }
      else {
        log.debug("Successful bid on item {}", auctionId)
        4
      }
    }

    val bidFormHtml = browser.get(bidFormURL(auctionId, bid))
    processBidFormHtml(bidFormHtml)
  }

  private[this] def resolveKey(key: String, map: Map[String, String]) =
    map.getOrElse(key, siteConfig.getString(key))

  private[this] def replaceVars(str: String, map: Map[String, String]) = {
    "\\{([^\\}]*)\\}".r.findAllIn(str).foldLeft(str) { (curr, key) =>
      curr.replace(key, resolveKey(key.substring(1, key.length - 1), map))
    }
  }

  private[this] val pageDumpingEnabled =
    ConfigFactory.load.getBoolean("ebay.debug.dump-unexpected-error-pages")

  private[this] lazy val dumpedPagesDir =
    ConfigFactory.load.getString("ebay.debug.dumped-pages-dir")

  private[this] def dumpErrorPage(filename: String, content: => String) =
    if(pageDumpingEnabled) new PrintStream(s"$dumpedPagesDir/$filename").use(_.println(content))
}
