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

  /** Sanity check the configuration, log descriptive error if invalid. */
  checkMandatoryConfigurationPresent(configuration)
  checkConfigurationValid(configuration)

  /** The application's recaptcha private key. */
  val privateKey: String = configuration.underlying.getString(PrivateKeyConfigProp)

  /** The application's recaptcha public key. */
  val publicKey: String = configuration.underlying.getString(PublicKeyConfigProp)

  /** The version of recaptcha API to use. */
  val apiVersion: Int = getApiVersion()

  /** The millisecond request timeout duration, when connecting to the recaptcha web API. */
  val requestTimeoutMs: Long = configuration.getMilliseconds(RequestTimeoutConfigProp)
              .getOrElse(RequestTimeoutMsDefault)

  /** The theme for the recaptcha widget to use (if any). */
  val theme: Option[String] = configuration.getString(ThemeConfigProp)

  // V1 Only Settings
  /** The language (if any) to use if browser doesn't support any languages supported by reCAPTCHA. */
  val defaultLanguage: String = configuration.getString(
          DefaultLanguageConfigProp, validValues = Some(supportedV1LanguageCodes))
              .getOrElse(DefaultLanguageDefault)

  /** Whether to use the secure (SSL) URL to access the verify API. */
  val useSecureVerifyUrl: Boolean = configuration.getBoolean(UseSecureVerifyUrlConfigProp)
              .getOrElse(false)

  /** Whether to use the secure (SSL) URL to render the reCAPCTHA widget. */
  val useSecureWidgetUrl: Boolean = configuration.getBoolean(UseSecureWidgetUrlConfigProp)
              .getOrElse(false)
  // End V1 Only Settings

  // V2 Only Settings
  /** The v2 captcha type to use (if any). */
  val captchaType: String = configuration.getString(CaptchaTypeConfigProp, validValues =
      Some(Set("image", "audio"))).getOrElse(CaptchaTypeDefault)

  /** The captcha size to use (if any). */
  val captchaSize: String = configuration.getString(CaptchaSizeConfigProp, validValues =
      Some(Set("normal", "compact"))).getOrElse(CaptchaSizeDefault)
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
        var configurationValid = true

        // check the api version first
        var apiVersion = getApiVersion()
        if (apiVersion == 1) {
            // check API version 1 configuration...

	        // keep going so all invalid items get logged, not just the first one...
	        booleanConfiguration.foreach(key => {
	            if (!validateBoolean(key, configuration)) {
	                configurationValid = false
	            }
	        })

	        // sanity check the default language (if set) is a supported one
	        // only log as a warning since the supported languages might be out of date
	        configuration.getString(DefaultLanguageConfigProp).foreach(key => {
	        	if (!supportedV1LanguageCodes.contains(key)) {
	        	    logger.warn(s"The default language you have set ($key) is not supported by reCAPTCHA")
	        	}
	        })
        }

        if (!configurationValid) {
            val message = "Configuration invalid. Please check the module documentation " +
                    "and correct your application.conf file."
            logger.error(message)
        }
    }

    /**
     * Obtains the recaptcha API version configured.
     * @return The version number, or <code>None</code> if invalid (error will have been logged).
     */
    private def getApiVersion(): Int = {
        var apiVersion = 0
        var versionString =
            configuration.getString(ApiVersionConfigProp).getOrElse("0")

        try {
            apiVersion = versionString.toInt
        } catch {
            case ex: NumberFormatException =>
                logger.error(s"$versionString is not a valid recaptcha API version")
                throw new ConfigException.Missing(ApiVersionConfigProp)
        }
        if (apiVersion < 1 || apiVersion > 2) {
            logger.error(s"recaptcha API version $apiVersion is not supported")
            throw new ConfigException.Missing(ApiVersionConfigProp)
        }

        return apiVersion
    }

    /**
     * Validates a boolean configuration setting, if present.
     * @param setting		The setting
     * @param configuration	The configuration
     * @return Whether setting is a valid boolean
     */
    private def validateBoolean(setting: String, configuration: Configuration): Boolean = {
        // booleans can be true/false/yes/no but only lower case
        val validValues = Seq("true", "false", "yes", "no")
        configuration.getString(setting).map { value => {
            if (!validValues.contains(value)) {
	            logger.error(setting + " must be true/false/yes/no, not " + value)
	            return false
            }
        }}
        return true
    }
}

object RecaptchaSettings {
	private val root = "recaptcha"

	/** The application's recaptcha private key. */
	val PrivateKeyConfigProp = s"$root.privateKey"

	/** The application's recaptcha public key. */
	val PublicKeyConfigProp = s"$root.publicKey"

	/** The version of recaptcha API to use. */
	val ApiVersionConfigProp = s"$root.apiVersion"

	/** The millisecond request timeout duration, when connecting to the recaptcha web API. */
	val RequestTimeoutConfigProp = s"$root.requestTimeout"

	/** The theme for the recaptcha widget to use (if any). */
	val ThemeConfigProp = s"$root.theme"

	/** The language (if any) to use if browser doesn't support any languages supported by reCAPTCHA. */
	val DefaultLanguageConfigProp = s"$root.defaultLanguage"

	/** Whether to use the secure (SSL) URL to access the verify API. */
	val UseSecureVerifyUrlConfigProp = s"$root.useSecureVerifyUrl"

	/** Whether to use the secure (SSL) URL to render the reCAPCTHA widget. */
	val UseSecureWidgetUrlConfigProp = s"$root.useSecureWidgetUrl"

	/** The v2 captcha type to use (if any). */
	val CaptchaTypeConfigProp = s"$root.type"

	/** The captcha size to use (if any). */
	val CaptchaSizeConfigProp = s"$root.size"

	// Default Values
	import scala.concurrent.duration._
	val RequestTimeoutMsDefault = 10.seconds.toMillis
	val DefaultLanguageDefault = "en"
	val CaptchaTypeDefault = "image"
	val CaptchaSizeDefault = "normal"

    /** The mandatory configuration items that must exist for this module to work. */
    private[recaptcha] val mandatoryConfiguration =
        Seq(PrivateKeyConfigProp, PublicKeyConfigProp, ApiVersionConfigProp)

    /** The boolean configuration items. */
    private[recaptcha] val booleanConfiguration =
        Seq(UseSecureVerifyUrlConfigProp, UseSecureWidgetUrlConfigProp)

    /** As taken from Google reCAPTCHA documentation, 23/07/2014. This needs to be kept up to date. */
    val supportedV1LanguageCodes = Set( "en", "nl", "fr", "de", "pt", "ru", "es", "tr" )
}
