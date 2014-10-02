package net.ruippeixotog.ebaysniper.browser

import com.typesafe.config.Config
import net.ruippeixotog.ebaysniper.browser.Browser._
import net.ruippeixotog.ebaysniper.util.Logging
import org.jsoup.nodes.Document

import scala.collection.convert.WrapAsScala._

class EbayLoginManager(siteConf: Config, username: String, password: String)(
    implicit browser: Browser) extends Logging {
  
  val loginConf = siteConf.getConfig("login")

  def login(): Boolean = {
    if(browser.cookies.contains("shs")) true
    else forceLogin()
  }

  def forceLogin(): Boolean = {
    browser.cookies.clear()
    log.debug("Getting the sign in cookie for {}", siteConf.getString("name"))

    val signInHtml = browser.get(loginConf.getString("sign-in-page"))
    val signInForm = signInHtml.select("form").filter(_.select("input[name=pass]").nonEmpty).head

    val signInData = signInForm.extractFormData + ("userid" -> username) + ("pass" -> password)
    val confirmHtml = browser.post(signInForm.attr("action"), signInData)

    val isSuccess = validLogin(confirmHtml)
    if (isSuccess) {
      val hidUrl = confirmHtml.select("form input[name=hidUrl]")(0).attr("value")

      if (!hidUrl.matches(loginConf.getString("valid-success-uri")))
        log.warn("Security checks out, but no My eBay form link on final page")

      log.info("Login successful")
    }

    isSuccess
  }

  private[this] def validLogin(doc: Document): Boolean = {
    if(doc.baseUri.contains("FYPShow") || doc.title == "Reset your password") {
      log.error("eBay is requesting that you change your password. You must change your password on eBay")
      false
    }
    else if (doc.egrep(loginConf.getString("verification-needed")).nonEmpty) {
      log.error("eBay's security monitoring has been triggered and temporarily requires human " +
          "intervention to log in")
      false
    }
    else if (doc.egrep(loginConf.getString("invalid-credentials")).nonEmpty) {
      log.error("Your sign in information is not correct")
      false
    } else true
  }
}
