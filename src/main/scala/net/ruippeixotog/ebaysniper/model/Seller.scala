package net.ruippeixotog.ebaysniper.model

case class Seller(id: String, feedback: Int, positivePercentage: Double)

object Seller {
  def apply(a: com.jbidwatcher.auction.Seller): Seller = Seller(
    id = a.getSeller,
    feedback = a.getFeedback,
    positivePercentage = a.getPositivePercentage.dropRight(1).toDouble)
}
