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
import play.api.{Configuration, Logger}
import javax.inject.{Inject, Singleton}

/**
 * Module configuration.
 *
 * @author chrisnappin, gmalouf
 */
@Singleton
class RecaptchaSettings @Inject() (configuration: Configuration) {
    import RecaptchaSettings._

    val logger = Logger(this.getClass())

    /** The application's recaptcha private key. */
    val privateKey: String = configuration.underlying.getString(PrivateKeyConfigProp)

    /** The application's recaptcha public key. */
    val publicKey: String = configuration.underlying.getString(PublicKeyConfigProp)

    /** The millisecond request timeout duration, when connecting to the recaptcha web API. */
    val requestTimeoutMs: Long = configuration.getMilliseconds(RequestTimeoutConfigProp)
              .getOrElse(RequestTimeoutMsDefault)

    /** The theme for the recaptcha widget to use (if any). */
    val theme: Option[String] = configuration.getString(ThemeConfigProp)


    /** The captcha type to use (if any). */
    val captchaType: String = configuration.getString(CaptchaTypeConfigProp, validValues =
            Some(Set("image", "audio"))).getOrElse(CaptchaTypeDefault)

    /** The captcha size to use (if any). */
    val captchaSize: String = configuration.getString(CaptchaSizeConfigProp, validValues =
            Some(Set("normal", "compact"))).getOrElse(CaptchaSizeDefault)

    /** The captcha language mode to use (if any). */
    val languageMode: String = configuration.getString(LanguageModeConfigProp,
            validValues = Some(Set("auto", "play", "force"))).getOrElse(LanguageModeDefault)

    val forceLanguage: Option[String] = configuration.getString(ForceLanguageConfigProp)

    /** Sanity check the configuration, log descriptive error if invalid. */
    checkMandatoryConfigurationPresent(configuration)
    checkConfigurationValid(configuration)

    /**
	 * Get the URL (secure or insecure) for the verify API.
	 *
	 * @return The URL
	 */
    def verifyUrl: String = "https://www.google.com/recaptcha/api/siteverify"

	/**
	 * Get the URL (secure or insecure if v1, always secure if v2) for the widget (script).
	 *
	 * @return The URL
	 */
	def widgetScriptUrl: String = "https://www.google.com/recaptcha/api.js"

	/**
	 * Get the URL (secure or insecure if v1, always secure if v2) for the widget (no script).
	 *
	 * @return The URL
	 */
	def widgetNoScriptUrl: String = "https://www.google.com/recaptcha/api/fallback"

    /**
     * Check whether the mandatory configuration is present. If not a suitable error log
     * message will be written.
     * @param configuration		The configuration to check
     * @throws RecaptchaConfigurationException If configuration is invalid
     */
    private def checkMandatoryConfigurationPresent(configuration: Configuration): Unit = {
        var mandatoryConfigurationPresent = true

        // keep looping so all missing items get logged, not just the first one...
        mandatoryConfiguration.foreach(key => {
            if (!configuration.keys.contains(key)) {
                logger.error(key + " not found in application configuration")
                mandatoryConfigurationPresent = false
            }
        })

        if (!mandatoryConfigurationPresent) {
            val message = "Mandatory configuration missing. Please check the module " +
                    "documentation and add the missing items to your application.conf file."
            logger.error(message)
        }
    }

      /**
     * Check whether the configuration is valid. If not a suitable error log message will
     * be written.
     * @param configuration		The configuration to check
     * @throws RecaptchaConfigurationException If configuration is invalid
     */
    private def checkConfigurationValid(configuration: Configuration): Unit = {
        // if languageMode is set to "force" then "forceLanguage" must be defined
        if (languageMode == "force" && forceLanguage.isEmpty) {
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

	// Default Values
	import scala.concurrent.duration._
	val RequestTimeoutMsDefault = 10.seconds.toMillis
	val DefaultLanguageDefault = "en"
	val CaptchaTypeDefault = "image"
	val CaptchaSizeDefault = "normal"
	val LanguageModeDefault = "auto"

    /** The mandatory configuration items that must exist for this module to work. */
    private[recaptcha] val mandatoryConfiguration = Seq(PrivateKeyConfigProp, PublicKeyConfigProp)
}
