package net.ruippeixotog.ebaysniper

import java.util.{ Date => JDate }

import com.github.nscala_time.time.Imports._
import net.ruippeixotog.ebaysniper.model._
import spray.json.DefaultJsonProtocol._
import spray.json._

object JsonProtocol {

  implicit class SafeJsonConvertible[T](val obj: T) extends AnyVal {
    def safeJson(implicit writer: JsonWriter[T]): JsValue =
      if (obj == null) JsNull else writer.write(obj)
  }

  implicit def optionWriter[T: JsonWriter] = new JsonWriter[Option[T]] {
    override def write(opt: Option[T]) = opt match {
      case Some(x) => x.toJson
      case None => JsNull
    }
  }

  implicit object IntegerJsonProtocol extends JsonWriter[Integer] {
    override def write(n: Integer) = JsNumber(n)
  }

  implicit object JDateJsonProtocol extends JsonWriter[JDate] {
    override def write(date: JDate) = JsString(date.toLocalDateTime.toString)
  }

  implicit object DateTimeJsonProtocol extends JsonWriter[DateTime] {
    override def write(date: DateTime) = JsString(date.toString)
  }

  implicit object CurrencyJsonProtocol extends JsonWriter[Currency] {
    def write(cur: Currency) = JsString(cur.symbol + " " + cur.value)
  }

  implicit object SellerJsonProtocol extends RootJsonWriter[Seller] {
    override def write(s: Seller) = Map(
      "id" -> s.id.safeJson,
      "feedback" -> s.feedback.safeJson,
      "positivePercentage" -> s.positivePercentage.safeJson
    ).toJson
  }

  implicit object AuctionInfoJsonProtocol extends RootJsonWriter[Auction] {
    def write(a: Auction) = Map(
      "id" -> a.id.safeJson,
      "title" -> a.title.safeJson,
      "endingAt" -> a.endingAt.safeJson,
      "ended" -> a.ended.toJson,
      "seller" -> a.seller.safeJson,
      "currentBid" -> a.currentBid.safeJson,
      "bidCount" -> a.bidCount.safeJson,
      "buyNowPrice" -> a.buyNowPrice.safeJson,
      "location" -> a.location.safeJson,
      "shippingCost" -> a.shippingCost.safeJson,
      "defaultCurrency" -> a.defaultCurrency.safeJson,
      "thumbnailUrl" -> a.thumbnailUrl.safeJson
    ).toJson
  }

  implicit object SnipeInfoJsonProtocol extends RootJsonFormat[SnipeInfo] {

    override def read(json: JsValue) = {
      val jObj = json.asJsObject.fields

      val auctionId = jObj.get("auctionId") match {
        case Some(JsString(aId)) => aId
        case _ => null
      }

      val description = jObj.get("description") match {
        case Some(JsString(desc)) => desc
        case _ => ""
      }

      val bid = jObj.get("bid") match {
        case Some(JsNumber(b)) => Currency("USD", b.toDouble)
        case Some(JsString(str)) => Currency.parse(str)
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

      SnipeInfo(auctionId, description, bid, quantity, snipeTime)
    }

    override def write(info: SnipeInfo) = Map(
      "auctionId" -> info.auctionId.safeJson,
      "description" -> info.description.safeJson,
      "bid" -> info.bid.safeJson,
      "quantity" -> info.quantity.safeJson,
      "snipeTime" -> info.snipeTime.orNull.safeJson
    ).toJson
  }
}
