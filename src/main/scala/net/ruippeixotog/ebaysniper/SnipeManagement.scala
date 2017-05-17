package net.ruippeixotog.ebaysniper

import java.io.{ File, PrintStream }
import java.util.concurrent.CancellationException

import net.ruippeixotog.ebaysniper.JsonProtocol._
import net.ruippeixotog.ebaysniper.SnipeServer._
import net.ruippeixotog.ebaysniper.ebay.BiddingClient
import net.ruippeixotog.ebaysniper.ebay.BiddingClient.BidStatus
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.ExecutionContext
import scala.io.Source
import scala.util.{ Failure, Success }

trait SnipeManagement {
  implicit def ec: ExecutionContext
  implicit def ebay: BiddingClient

  val snipesFile: Option[File] = None

  // create the data structures and utilities for managing the snipe data
  private[this] var _snipes = Map.empty[String, Snipe]
  def snipes = _snipes

  lazy val scheduler = SnipeScheduler()

  def loadSnipesFromFile() {
    snipes.values.foreach(_.cancel())

    snipesFile match {
      case Some(file) =>
        log.info("Using {} for persisting snipe data", file)

        file.getParentFile.mkdirs()
        if (file.exists()) {
          for (sInfo <- Source.fromFile(file).mkString.parseJson.convertTo[List[SnipeInfo]]) {
            registerAndActivate(new Snipe(sInfo))
          }
        }

      case None =>
        log.warn("No persistent file for storing snipe data was specified. Any configured snipe " +
          "will be lost if the server is terminated")
    }
  }

  def saveSnipesToFile() {
    snipesFile.foreach { file =>
      val out = new PrintStream(file)
      out.println(snipes.values.map(_.info).toJson.compactPrint)
      out.close()
    }
  }

  def registerAndActivate(snipe: Snipe) {
    val auctionId = snipe.info.auctionId

    // cancel the previous snipe, if exists
    snipes.get(auctionId).foreach(_.cancel())

    // add the new snipe to the map
    _snipes += (auctionId -> snipe)
    saveSnipesToFile()

    snipe.activate().onComplete { res =>
      res match {
        case Success(status) if BidStatus.isSuccess(status) =>
          log.info("Completed snipe {} successfully - {}", snipe.info, BidStatus.statusMessage(status), null)

        case Success(status) =>
          log.warn("Completed snipe {} with errors - {}", snipe.info, BidStatus.statusMessage(status), null)

        case Failure(e: CancellationException) =>
          log.info("The snipe {} was cancelled", snipe.info)

        case Failure(e) =>
          log.error(s"The snipe ${snipe.info} failed", e)
      }
      if (snipes.get(auctionId) == Some(snipe)) {
        _snipes -= auctionId
        saveSnipesToFile()
      }
    }
  }
}
