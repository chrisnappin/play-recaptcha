/*
 * Copyright 2017 Chris Nappin
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
import play.api.{Configuration, Logger}
import javax.inject.{Inject, Singleton}

/** Module configuration.
  *
  * @author
  *   chrisnappin, gmalouf
  */
@Singleton
class RecaptchaSettings @Inject() (configuration: Configuration) {
  import RecaptchaSettings.*

  private val logger = Logger(this.getClass())

  /** The application's recaptcha private key. */
  val privateKey: String = configuration.get[String](PrivateKeyConfigProp)

  /** The application's recaptcha public key. */
  val publicKey: String = configuration.get[String](PublicKeyConfigProp)

  /** The millisecond request timeout duration, when connecting to the recaptcha web API. */
  val requestTimeoutMs: Long =
    configuration.getOptional[String](RequestTimeoutConfigProp) match {
      case Some(_) => configuration.getMillis(RequestTimeoutConfigProp)
      case None    => RequestTimeoutMsDefault.toMillis
    }

  /** The theme for the recaptcha widget to use (if any). */
  val theme: Option[String] = configuration.getOptional[String](ThemeConfigProp)

  /** The captcha type to use (if any). */
  val captchaType: String =
    configuration.getOptional[String](CaptchaTypeConfigProp) match {
      case Some(_) =>
        configuration.getAndValidate[String](
          CaptchaTypeConfigProp,
          values = Set("image", "audio")
        )
      case None => CaptchaTypeDefault
    }

  /** The captcha size to use (if any). */
  val captchaSize: String =
    configuration.getOptional[String](CaptchaSizeConfigProp) match {
      case Some(_) =>
        configuration.getAndValidate[String](
          CaptchaSizeConfigProp,
          values = Set("normal", "compact")
        )
      case None => CaptchaSizeDefault
    }

  /** The captcha language mode to use (if any). */
  val languageMode: String =
    configuration.getOptional[String](LanguageModeConfigProp) match {
      case Some(_) =>
        configuration.getAndValidate[String](
          LanguageModeConfigProp,
          values = Set("auto", "play", "force")
        )
      case None => LanguageModeDefault
    }

  /** The language to force use of (if any). */
  val forceLanguage: Option[String] = configuration.getOptional[String](ForceLanguageConfigProp)

  /** The content security policy to use (if any). */
  val contentSecurityPolicy: String = configuration
    .getOptional[String](ContentSecurityPolicyProp)
    .getOrElse(ContentSecurityPolicyDefault)

  /** The length of nonces to generate (if any). */
  val nonceLength: Int = configuration
    .getOptional[Int](NonceLengthProp)
    .getOrElse(NonceLengthDefault)

  /** The seed to use for nonce generation (if any). */
  val nonceSeed: Option[Long] = configuration.getOptional[Long](NonceSeedProp)

  /** Sanity check the configuration, log descriptive error if invalid. */
  checkMandatoryConfigurationPresent(configuration)
  checkConfigurationValid(configuration)

  /** Get the URL (secure or insecure) for the verify API.
    *
    * @return
    *   The URL
    */
  def verifyUrl: String = "https://www.google.com/recaptcha/api/siteverify"

  /** Get the URL (secure or insecure if v1, always secure if v2) for the widget (script).
    *
    * @return
    *   The URL
    */
  def widgetScriptUrl: String = "https://www.google.com/recaptcha/api.js"

  /** Get the URL (secure or insecure if v1, always secure if v2) for the widget (no script).
    *
    * @return
    *   The URL
    */
  def widgetNoScriptUrl: String = "https://www.google.com/recaptcha/api/fallback"

  /** Check whether the mandatory configuration is present. If not a suitable error log message will
    * be written.
    * @param configuration
    *   The configuration to check
    */
  private def checkMandatoryConfigurationPresent(configuration: Configuration): Unit = {
    var mandatoryConfigurationPresent = true

    // keep looping so all missing items get logged, not just the first one...
    mandatoryConfiguration.foreach(key => {
      if !configuration.keys.contains(key) then {
        logger.error(key + " not found in application configuration")
        mandatoryConfigurationPresent = false
      }
    })

    if !mandatoryConfigurationPresent then {
      logger.error(
        "Mandatory configuration missing. Please check the module " +
          "documentation and add the missing items to your application.conf file."
      )
    }
  }

  /** Check whether the configuration is valid. If not a suitable error log message will be written.
    * @param configuration
    *   The configuration to check
    * @throws ConfigException.Missing
    *   If configuration is invalid
    */
  private def checkConfigurationValid(configuration: Configuration): Unit = {
    // if languageMode is set to "force" then "forceLanguage" must be defined
    if languageMode == "force" && forceLanguage.isEmpty then {
      logger.error("If languageMode is \"force\" then forceLanguage must be defined")
      throw new ConfigException.Missing(LanguageModeConfigProp)
    }
  }
}

object RecaptchaSettings {
  private val root = "recaptcha"

  /** The application's recaptcha private key. */
  val PrivateKeyConfigProp = s"$root.privateKey"

  /** The application's recaptcha public key. */
  val PublicKeyConfigProp = s"$root.publicKey"

  /** The millisecond request timeout duration, when connecting to the recaptcha web API. */
  val RequestTimeoutConfigProp = s"$root.requestTimeout"

  /** The theme for the recaptcha widget to use (if any). */
  val ThemeConfigProp = s"$root.theme"

  /** The captcha type to use (if any). */
  val CaptchaTypeConfigProp = s"$root.type"

  /** The captcha size to use (if any). */
  val CaptchaSizeConfigProp = s"$root.size"

  /** The language mode to use (if any). */
  val LanguageModeConfigProp = s"$root.languageMode"

  /** The forced language value to use (if any). */
  val ForceLanguageConfigProp = s"$root.forceLanguage"

  /** The Content Security Policy to use (if any). */
  val ContentSecurityPolicyProp = s"$root.nonceAction.contentSecurityPolicy"

  /** The length of the nonces to generate (if any). */
  val NonceLengthProp = s"$root.nonceAction.nonceLength"

  /** The nonce generation seed (if any). */
  val NonceSeedProp = s"$root.nonceAction.nonceSeed"

  // Default Values
  import scala.concurrent.duration.*
  val RequestTimeoutMsDefault = 10.seconds
  val DefaultLanguageDefault = "en"
  val CaptchaTypeDefault = "image"
  val CaptchaSizeDefault = "normal"
  val LanguageModeDefault = "auto"
  val ContentSecurityPolicyDefault =
    "default-src 'self'; script-src 'self' 'nonce-{nonce}'; " +
      "style-src 'self' 'unsafe-inline'; frame-src https://www.google.com/recaptcha/;"
  val NonceLengthDefault = 20

  /** The mandatory configuration items that must exist for this module to work. */
  private[recaptcha] val mandatoryConfiguration = Seq(PrivateKeyConfigProp, PublicKeyConfigProp)
}
