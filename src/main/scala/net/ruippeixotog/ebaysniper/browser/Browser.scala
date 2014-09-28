package net.ruippeixotog.ebaysniper.browser

import com.github.nscala_time.time.Imports._
import com.typesafe.config.Config
import net.ruippeixotog.ebaysniper.util.Logging
import org.jsoup.Connection.Method._
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import org.jsoup.{Connection, Jsoup}

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable.{Map => MutableMap}

class Browser extends Logging {
  val cookies = MutableMap.empty[String, String]

  def get(url: String) = execute(url, _.method(GET))
  def post(url: String, form: Map[String, String]) = execute(url, _.method(POST).data(form))

  private[this] def execute(url: String, conn: Connection => Connection): Document =
    process(conn(Jsoup.connect(url).cookies(cookies)))

  private[this] def process(conn: Connection) = {
    val res = conn.execute()
    lazy val doc = res.parse()

    cookies ++= res.cookies()

    val redirectUrl =
      if(res.hasHeader("Location")) Some(res.header("Location"))
      else doc.select("head meta[http-equiv=refresh]").headOption.map { e =>
        e.attr("content").split(";").find(_.startsWith("url")).head.split("=")(1)
      }

    redirectUrl match {
      case None => doc
      case Some(url) =>
        log.info("Redirecting from {} to {}", conn.request.url, res, null)
        get(url)
    }
  }
}

object Browser {

  trait JsoupQueryable {
    def outerHtml: String
    def select(cssQuery: String): Elements

    def egrep(text: String): Option[String] = text.r.findFirstIn(outerHtml)

    def extractFormData: Map[String, String] = select("input").map { e =>
      e.attr("name") -> e.attr("value")
    }.toMap

    def selectFromConfig(queryConf: Config): Any = {
      val q = select(queryConf.getString("query"))

      if(q.isEmpty) None
      else Some {
        val content =
          if(queryConf.hasPath("attr")) q.attr(queryConf.getString("attr")) else q.text()

        if(queryConf.hasPath("date-format"))
          DateTimeFormat.forPattern(queryConf.getString("date-format")).parseDateTime(content)
        else if(queryConf.hasPath("regex-format"))
          queryConf.getString("regex-format").r.findFirstIn(content).get
        else content
      }
    }
  }

  implicit class ElementJsoupQueryable(val elem: Element) extends JsoupQueryable {
    def outerHtml = elem.outerHtml
    def select(cssQuery: String) = elem.select(cssQuery)
  }

  implicit class ElementsJsoupQueryable(val elems: Elements) extends JsoupQueryable {
    def outerHtml = elems.outerHtml
    def select(cssQuery: String) = elems.select(cssQuery)
  }
}
