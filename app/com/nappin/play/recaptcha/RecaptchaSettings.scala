/*
 * Copyright 2016 Chris Nappin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nappin.play.recaptcha

import com.typesafe.config.ConfigException
import play.api.Configuration
import javax.inject.{Inject, Singleton}

/**
 * Module configuration.
 *
 * @author chrisnappin, gmalouf
 */
@Singleton
class RecaptchaSettings @Inject() (configuration: Configuration) {
  import RecaptchaSettings._

  /** The application's recaptcha private key. */
  val privateKey: String = configuration.underlying.getString(PrivateKeyConfigProp)

  /** The application's recaptcha public key. */
  val publicKey: String = configuration.underlying.getString(PublicKeyConfigProp)

  /** The version of recaptcha API to use. */
  val apiVersion: Int = configuration.getString(ApiVersionConfigProp, validValues =
      Some(Set("1", "2"))).map(_.toInt).getOrElse(throw new ConfigException.Missing(ApiVersionConfigProp))

  /** The millisecond request timeout duration, when connecting to the recaptcha web API. */
  val requestTimeoutMs: Long = configuration.getMilliseconds(RequestTimeoutConfigProp).getOrElse(RequestTimeoutMsDefault)

  /** The theme for the recaptcha widget to use (if any). */
  val theme: Option[String] = configuration.getString(ThemeConfigProp)

  // V1 Only Settings
  /** The language (if any) to use if browser doesn't support any languages supported by reCAPTCHA. */
  val defaultLanguage: String = configuration.getString(DefaultLanguageConfigProp, validValues = Some(Set(
    "en", "nl", "fr", "de", "pt", "ru", "es", "tr"))).getOrElse(DefaultLanguageDefault)

  /** Whether to use the secure (SSL) URL to access the verify API. */
  val useSecureVerifyUrl: Boolean = configuration.getBoolean(UseSecureVerifyUrlConfigProp).getOrElse(false)

  /** Whether to use the secure (SSL) URL to render the reCAPCTHA widget. */
  val useSecureWidgetUrl: Boolean = configuration.getBoolean(UseSecureWidgetUrlConfigProp).getOrElse(false)
  // End V1 Only Settings

  // V2 Only Settings
  /** The v2 captcha type to use (if any). */
  val captchaType: String = configuration.getString(CaptchaTypeConfigProp, validValues =
      Some(Set("image", "audio"))).getOrElse(CaptchaTypeDefault)
  // end V2 Only Settings

  val isApiVersion1 = apiVersion == 1

  private val verifyUrlPrefix = toPrefix(useSecureVerifyUrl)
  private val widgetUrlPrefix = toPrefix(useSecureWidgetUrl)

  /**
    * Get the URL prefix (secure or insecure) as specified by the configuration flag.
    *
    * @param isSecure		Flag indicating whether use secure/insecure protocol
    * @return The prefix
    */
  private def toPrefix(isSecure: Boolean): String = if (isSecure) "https" else "http"


  /**
    * Get the URL (secure or insecure) for the verify API.
    *
    * @return The URL
    */
  def verifyUrl: String =
    if (isApiVersion1) s"$verifyUrlPrefix://www.google.com/recaptcha/api/verify"
    else "https://www.google.com/recaptcha/api/siteverify"

  /**
    * Get the URL (secure or insecure if v1, always secure if v2) for the widget (script).
    *
    * @return The URL
    */
  def widgetScriptUrl: String =
    if (isApiVersion1) s"$widgetUrlPrefix://www.google.com/recaptcha/api/challenge"
    else "https://www.google.com/recaptcha/api.js"

  /**
    * Get the URL (secure or insecure if v1, always secure if v2) for the widget (no script).
    *
    * @return The URL
    */
  def widgetNoScriptUrl: String =
    if (isApiVersion1) s"$widgetUrlPrefix://www.google.com/recaptcha/api/noscript"
    else "https://www.google.com/recaptcha/api/fallback"

}

object RecaptchaSettings {
  val RecaptchaConfigRoot = "recaptcha"

  val PrivateKeyConfigProp = s"$RecaptchaConfigRoot.privateKey"
  val PublicKeyConfigProp = s"$RecaptchaConfigRoot.publicKey"
  val ApiVersionConfigProp = s"$RecaptchaConfigRoot.apiVersion"
  val RequestTimeoutConfigProp = s"$RecaptchaConfigRoot.requestTimeout"
  val ThemeConfigProp = s"$RecaptchaConfigRoot.theme"
  val DefaultLanguageConfigProp = s"$RecaptchaConfigRoot.defaultLanguage"
  val UseSecureVerifyUrlConfigProp = s"$RecaptchaConfigRoot.useSecureVerifyUrl"
  val UseSecureWidgetUrlConfigProp = s"$RecaptchaConfigRoot.useSecureWidgetUrl"
  val CaptchaTypeConfigProp = s"$RecaptchaConfigRoot.type"

  // Default Values
  import scala.concurrent.duration._
  val RequestTimeoutMsDefault = 10.seconds.toMillis
  val DefaultLanguageDefault = "en"
  val CaptchaTypeDefault = "image"
}
