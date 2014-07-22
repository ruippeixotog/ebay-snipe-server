package net.ruippeixotog.ebaysniper

import java.io.{File, PrintStream}
import java.util.concurrent.CancellationException

import net.ruippeixotog.ebaysniper.JsonProtocol._
import net.ruippeixotog.ebaysniper.SnipeServer._
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.concurrent.ExecutionContext
import scala.io.Source
import scala.util.{Failure, Success}

trait SnipeManagement {
  implicit def executionContext: ExecutionContext

  val snipesFile: Option[String] = None

  // create the data structures and utilities for managing the snipe data
  private[this] var _snipes = Map.empty[String, Snipe]
  def snipes = _snipes

  val scheduler = SnipeScheduler()

  def loadSnipesFromFile() {
    snipes.values.foreach(_.cancel())

    _snipes = snipesFile match {
      case Some(file) =>
        log.info(s"Using {} for persisting snipe data", file)
        if(new File(file).exists()) {
          Source.fromFile(file).mkString.parseJson.convertTo[List[SnipeInfo]].map { sInfo =>
            val snipe = new Snipe(sInfo)
            snipe.activate()
            sInfo.auctionId -> snipe
          }.toMap
        } else Map.empty[String, Snipe]

      case None =>
        log.warn("No persistent file for storing snipe data was specified. Any configured snipe " +
          "will be lost if the server is terminated")
        Map.empty[String, Snipe]
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
    _snipes += (snipe.info.auctionId -> snipe)
    saveSnipesToFile()

    snipe.activate().onComplete { res =>
      res match {
        case Success(status) =>
          log.info("Completed snipe {} with status {}", snipe.info, status)

        case Failure(e: CancellationException) =>
          log.info("The snipe {} was cancelled", snipe.info)

        case Failure(e) => log.error(s"The snipe ${snipe.info} failed", e)
      }
      _snipes -= snipe.info.auctionId
      saveSnipesToFile()
    }
  }
}
