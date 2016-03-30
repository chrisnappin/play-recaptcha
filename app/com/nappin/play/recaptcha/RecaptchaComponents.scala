package com.nappin.play.recaptcha

import play.api.Configuration
import play.api.libs.ws.WSClient

/**
  * Injection helper for RecaptchaComponents
  */
trait RecaptchaComponents {
  def configuration: Configuration
  def wsClient: WSClient

  lazy val recaptchaSettings = new RecaptchaSettings(configuration)
  lazy val responseParser = new ResponseParser()
  lazy val recaptchaVerifier = new RecaptchaVerifier(recaptchaSettings, responseParser, wsClient)
  lazy val recaptchaWidgetHelper = new WidgetHelper(recaptchaSettings)
}
