package net.ruippeixotog.ebaysniper

import java.util.{Date => JDate}

import com.github.nscala_time.time.Imports._
import com.jbidwatcher.auction.{AuctionInfo, Seller}
import com.jbidwatcher.util.Currency
import spray.json.DefaultJsonProtocol._
import spray.json._

object JsonProtocol {

  implicit def safeAnyJsonConvert[T](obj: T) = new {
    def safeJson(implicit writer: JsonWriter[T]): JsValue =
      if(obj == null) JsNull else writer.write(obj)
  }

  implicit object IntegerJsonProtocol extends JsonWriter[Integer] {
    override def write(n: Integer) = JsNumber(n)
  }

  implicit object JDateJsonProtocol extends JsonWriter[JDate] {
    override def write(date: JDate) = JsString(date.toLocalDateTime.toString)
  }

  implicit object CurrencyJsonProtocol extends JsonWriter[Currency] {
    def write(cur: Currency) = if(cur.getCurrencyType == Currency.NONE) JsNull
      else JsString(cur.fullCurrencyName + " " + cur.getValueString)
  }

  implicit object SellerJsonProtocol extends RootJsonWriter[Seller] {
    override def write(s: Seller) = Map(
      "id" -> s.getId.safeJson,
      "name" -> s.getSeller.safeJson,
      "feedback" -> s.getFeedback.safeJson,
      "positivePercentage" -> s.getPositivePercentage.safeJson
    ).toJson
  }

  implicit object AuctionInfoJsonProtocol extends RootJsonWriter[AuctionInfo] {
    def write(a: AuctionInfo) = Map(
      "id" -> a.getIdentifier.safeJson,
      "title" -> a.getTitle.safeJson,
      "endingAt" -> a.getEndDate.safeJson,
      "ended" -> a.isComplete.toJson,
      "seller" -> a.getSeller.safeJson,
      "currentBid" -> a.getCurBid.safeJson,
      "highestBidder" -> a.getHighBidder.safeJson,
      "bidCount" -> a.getNumBids.safeJson,
      "bidderCount" -> a.getNumBidders.safeJson,
      "buyNowPrice" -> a.getBuyNow.safeJson,
      "location" -> a.getItemLocation.safeJson,
      "shippingCost" -> a.getShipping.safeJson,
      "defaultCurrency" -> a.getDefaultCurrency.fullCurrencyName.safeJson,
      "thumbnailUrl" -> a.getThumbnailURL.safeJson
    ).toJson
  }

  implicit object SnipeInfoJsonProcotol extends RootJsonFormat[SnipeInfo] {

    override def read(json: JsValue) = {
      val jObj = json.asJsObject.fields

      val auctionId = jObj.get("auctionId") match {
        case Some(JsString(aId)) => aId
        case _ => null
      }

      val bid = jObj.get("bid") match {
        case Some(JsNumber(b)) => Currency.getCurrency(Currency.US_DOLLAR, b.toDouble)
        case Some(JsString(str)) => Currency.getCurrency(str)
        case _ => throw new DeserializationException("A valid bid must be provided")
      }

      val quantity = jObj.get("quantity") match {
        case Some(JsNumber(qt)) => qt.toInt
        case _ => 1
      }

      val snipeTime = jObj.get("snipeTime") match {
        case Some(JsString(t)) => Some(t.toDateTime.toDate)
        case Some(JsNumber(n)) => Some(new JDate(n.toLong))
        case _ => None
      }

      SnipeInfo(auctionId, bid, quantity, snipeTime)
    }

    override def write(info: SnipeInfo) = Map(
      "auctionId" -> info.auctionId.safeJson,
      "bid" -> info.bid.safeJson,
      "quantity" -> info.quantity.safeJson,
      "snipeTime" -> info.snipeTime.orNull.safeJson
    ).toJson
  }
}
