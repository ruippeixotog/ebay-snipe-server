package net.ruippeixotog.ebaysniper.model

case class Currency(symbol: String, value: Double) {
  override def toString = s"$symbol $value"
}

object Currency {

 def parse(str: String) = {
   val symbol = (str.length to 1 by -1).toStream.flatMap { len =>
     SymbolTable.get(str.substring(0, len))
   }.headOption.getOrElse("UNK")

   val value = DoubleRegex.findFirstIn(str).get.toDouble
   Currency(symbol, value)
 }

  final val SymbolTable = Map(
    "USD" -> "USD", "US $" -> "USD", "US" -> "USD", "USD $" -> "USD", "$" -> "USD",
    "AU $" -> "AUD", "au$" -> "AUD", "AU" -> "AUD", "AUD" -> "AUD",
    "C" -> "CAD", "C $" -> "CAD", "CAD" -> "CAD", "c$" -> "CAD",
    "GBP" -> "GBP", "pound" -> "GBP", "\u00A3" -> "GBP", "&pound" ->"GBP",
    "Y" -> "JPY", "JPY" -> "JPY", "&yen" -> "JPY", "\u00A5" ->"JPY",
    "\u20AC" ->"EUR", "eur" -> "EUR", "EUR" -> "EUR", "Eur" -> "EUR",
    "NT$" -> "TWD", "nt$" -> "TWD", "NTD" -> "TWD",
    "HK$" -> "HKD", "hk$" -> "HKD", "HKD" -> "HKD",
    "INR" -> "INR", "inr" -> "INR")

  final val DoubleRegex = "\\d\\d*(\\.\\d)?\\d*".r
}
