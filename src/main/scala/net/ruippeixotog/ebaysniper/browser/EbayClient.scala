package net.ruippeixotog.ebaysniper.browser

import com.github.nscala_time.time.Imports._
import com.jbidwatcher.util.Currency
import com.typesafe.config.ConfigFactory
import net.ruippeixotog.ebaysniper.Snipe
import net.ruippeixotog.ebaysniper.browser.Browser._
import net.ruippeixotog.ebaysniper.model.{Auction, Seller}
import net.ruippeixotog.ebaysniper.util.Logging

import scala.reflect.ClassTag
import scala.util.{Success, Try}

import scala.collection.convert.WrapAsScala._

class EbayClient(site: String, username: String, password: String) extends BiddingClient with Logging {
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

  def bidFormURL(auctionId: String, bid: Currency) = {
    replaceVars(siteConfig.getString("bid-info.uri-template"),
      Map("auctionId" -> auctionId, "bidValue" -> bid.getValue.toString))
  }

  def bid(auctionId: String, bid: Currency, quantity: Int): Int = {
    loginMgr.login()
    log.debug("Bidding {} on item {}", bid, auctionId, null)

    val bidFormHtml = browser.get(bidFormURL(auctionId, bid))
    val bidForm = bidFormHtml.getElementById("reviewbid")

    if(bidForm != null && bidForm.select("input[name=confirmbid][type=submit]").nonEmpty) {
      val bidFormData = bidForm.extractFormData
      val confirmHtml = browser.post(bidForm.attr("action"), bidFormData)

      if(confirmHtml.egrep(siteConfig.getString("bid-confirm.success-message")).nonEmpty) {
        log.debug("Successful bid on item {}", auctionId)
        4
      }
      else {
        siteConfig.getConfigList("bid-confirm.error-statuses").toStream.flatMap { errorDef =>
          confirmHtml.selectFromConfig(errorDef.getConfig("select")).asInstanceOf[Option[String]].flatMap { content =>
            errorDef.getString("match").r.findFirstIn(content).map { _ =>
              val status = errorDef.getInt("status")
              log.warn("Bid on item {} not successful: {} (#{})",
                auctionId, Snipe.statusMessage(status), status.toString)
              status
            }
          }
        }.headOption.getOrElse(-1)
      }

    } else {
      siteConfig.getConfigList("bid-info.error-statuses").toStream.flatMap { errorDef =>
        bidFormHtml.selectFromConfig(errorDef.getConfig("select")).asInstanceOf[Option[String]].flatMap { content =>
          errorDef.getString("match").r.findFirstIn(content).map { _ =>
            val status = errorDef.getInt("status")
            log.warn("Bid on item {} not successful: {} (#{})",
              auctionId, Snipe.statusMessage(status), status.toString)
            status
          }
        }
      }.headOption.getOrElse(-1)
    }
  }

  private[this] def resolveKey(key: String, map: Map[String, String]) =
    map.getOrElse(key, siteConfig.getString(key))

  private[this] def replaceVars(str: String, map: Map[String, String]) = {
    "\\{([^\\}]*)\\}".r.findAllIn(str).foldLeft(str) { (curr, key) =>
      curr.replace(key, resolveKey(key.substring(1, key.length - 1), map))
    }
  }
}
