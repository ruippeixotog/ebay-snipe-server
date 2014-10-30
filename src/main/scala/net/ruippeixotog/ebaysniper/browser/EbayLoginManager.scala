package net.ruippeixotog.ebaysniper.browser

import com.typesafe.config.Config
import net.ruippeixotog.ebaysniper.util.Implicits._
import net.ruippeixotog.ebaysniper.util.Logging
import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.scraper.{ContentExtractors => Extract}
import net.ruippeixotog.scalascraper.util.Validated.{VFailure, VSuccess}

class EbayLoginManager(siteConf: Config, username: String, password: String)(
    implicit browser: Browser) extends Logging {

  implicit private[this] def defaultConf = siteConf

  def login(): Boolean = {
    if(browser.cookies.contains("shs")) true
    else forceLogin()
  }

  def loginUrl = siteConf.getString("login-form.uri-template").resolveVars()

  def forceLogin(): Boolean = {
    browser.cookies.clear()
    log.debug("Getting the sign in cookie for {}", siteConf.getString("name"))

    val (formData, signInAction) = browser.get(loginUrl) >> signInFormExtractor
    val signInData = formData + ("userid" -> username) + ("pass" -> password)

    browser.post(signInAction, signInData) ~/~ loginErrors match {
      case VFailure(status) =>
        log.error("A problem occurred while signing in ({})", status)
        false

      case VSuccess(doc) =>
        doc ~/~ loginWarnings match {
          case VFailure(status) =>
            log.warn("A warning occurred while signing in ({})", status)
          case _ =>
        }
        log.info("Login successful")
        true
    }
  }

  private[this] lazy val signInFormExtractor =
    Extract.formDataAndAction(siteConf.getString("login-form.form-query"))

  private[this] lazy val loginErrors = matchersAt[String](siteConf, "login-confirm.error-statuses")
  private[this] lazy val loginWarnings = matchersAt[String](siteConf, "login-confirm.warn-statuses")
}
