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

import org.apache.commons.lang3.StringEscapeUtils

import play.api.Logger
import play.api.i18n.{Lang, Messages}
import play.api.mvc.{AnyContent, Request}

import scala.collection.mutable.ListBuffer
import javax.inject.{Inject, Singleton}

/**
  * Helper functionality for the <code>recaptchaWidget</code> view template.
  *
  * @author chrisnappin
  * @param settings Recaptcha configuration settings
  */
@Singleton
class WidgetHelper @Inject() (settings: RecaptchaSettings) {

    val logger = Logger(this.getClass)

    /**
     * Determines whether API version 1 has been configured.
     * @return <code>true</code> if configured
     */
    def isApiVersion1: Boolean = settings.isApiVersion1


    /**
     * Returns the configured public key.
     * @return The public key
     */
    def publicKey: String = settings.publicKey

    /**
     * Returns the configured theme, or "light" if none defined
     * @return The theme to use
     */
    def v2Theme: String = settings.theme.getOrElse("light")


    /**
     * Returns the configured captcha type, or the default if none defined
     * @return The type to use
     */
    def v2Type: String = settings.captchaType

    /**
     * Returns the configured captcha size, or the default if none defined
     * @return The size to use
     */
    def v2Size: String = settings.captchaSize


    /**
     * Returns the widget script URL, with parameters (if applicable).
     * @param error			The error code (if any)
     * @param messages		The current i18n messages
     * @return The URL
     */
    def widgetScriptUrl(error: Option[String] = None)(implicit messages: Messages): String = {
        if (isApiVersion1) {
            // API v1 includes public key and error code (if any)
	        val errorSuffix = error.map("&error=" + _).getOrElse("")

          s"${settings.widgetScriptUrl}?k=$publicKey$errorSuffix"
        } else {
            val languageSuffix =
                if (settings.languageMode == "force") {
                    "?hl=" + settings.forceLanguage.get
                } else if (settings.languageMode == "play") {
                    "?hl=" + mapToV2Language(messages.lang)
                } else {
                    ""
                }

            s"${settings.widgetScriptUrl}$languageSuffix"
        }
    }

    /**
     * Returns the widget no-script URL, with public key and error code (if any).
     * @param error			The error code (if any)
     * @return The URL
     */
    def widgetNoScriptUrl(error: Option[String] = None): String = {
        if (isApiVersion1) {
            // API v1 includes public key and error code (if any)
            val errorSuffix = error.map("&error=" + _).getOrElse("")

            s"${settings.widgetNoScriptUrl}?k=$publicKey$errorSuffix"
        } else {
            // API v2 only includes public key
            s"${settings.widgetNoScriptUrl}?k=$publicKey"
        }
    }

    /**
     * Returns the <code>RecaptchaOptions</code> JavaScript declaration, with the appropriate
     * customisation options.
     * @param tabindex		The tabindex (if any)
     * @param request		The web request
     * @param messages		The current I18n messages
     * @return The JavaScript code
     */
    def recaptchaOptions(tabindex: Option[Int])(implicit request: Request[AnyContent],
            messages: Messages): String =
        new StringBuffer("var RecaptchaOptions = {\n")
            .append(s"  lang : '${preferredLanguage()}'")
            .append(settings.theme.fold(""){t => s",\n  theme : '$t'"})
            .append(tabindex.fold(""){t => s",\n  tabindex : $t"})
            .append(customTranslations().fold(""){t => s",\n  custom_translations : {\n$t\n  }"})
            .append("\n};").toString

    /**
     * Returns the most preferred language (as extracted from the <code>Accept-Language</code> HTTP
     * header in the request, if any) that is supported by reCAPTCHA. If no supported language is
     * found, returns the default language from configuration (if any), otherwise English ("en").
     * @param request		The web request
     * @return The language code
     */
    private[recaptcha] def preferredLanguage()(implicit request: Request[AnyContent]): String = {
        request.acceptLanguages.find(l => isSupportedLanguage(l.language))
        	.fold(settings.defaultLanguage) // if no supported language found
        		{lang => lang.language} // if a supported language was found
    }

    /**
     * Maps the current Play locale to the reCAPTCHA v2 language code.
     * @param lang The play locale
     * @return The language code, possibly with country code too
     */
    private def mapToV2Language(lang: Lang): String = {
        // list of language and country code combinations specifically supported by reCAPTCHA
        // (taken from https://developers.google.com/recaptcha/docs/language in October 2016)
        val supportedCountryLocales = Seq(
                Lang("zh", "HK"), Lang("zh", "CN"), Lang("zh", "TW"), Lang("en", "GB"), Lang("fr", "CA"),
                Lang("de", "AT"), Lang("de", "CH"), Lang("pr", "BR"), Lang("pr", "PT"), Lang("es", "419"))

        if (supportedCountryLocales.contains(lang)) {
            // use language and country code
            lang.language + "-" + lang.country
        } else {
            // just use the language code
            lang.language
        }
    }
    /**
     * Determines whether the specified language code is supported by reCAPTCHA.
     * @param languageCode		The language code
     * @return <code>true</code> if supported
     */
    def isSupportedLanguage(languageCode: String): Boolean = {
        RecaptchaSettings.supportedV1LanguageCodes.contains(languageCode)
    }

    /**
     * Get the custom translations (if any) formatted as a JavaScript dictionary of escaped strings.
     * @param messages		The current I18n messages
     * @return The custom translations (if any)
     */
    private def customTranslations()(implicit messages: Messages): Option[String] = {
        // maps play message names to javascript translation dictionary names
        val messageNames = Map(
                "recaptcha.visualChallenge" -> "visual_challenge",
                "recaptcha.audioChallenge" -> "audio_challenge",
                "recaptcha.refreshButton" -> "refresh_btn",
                "recaptcha.instructionsVisual" -> "instructions_visual",
                "recaptcha.instructionsAudio" -> "instructions_audio",
                "recaptcha.helpButton" -> "help_btn",
                "recaptcha.playAgain" -> "play_again",
                "recaptcha.cantHearThis" -> "cant_hear_this",
                "recaptcha.incorrectTryAgain" -> "incorrect_try_again",
                "recaptcha.imageAltText" -> "image_alt_text",
                "recaptcha.privacyAndTerms" -> "privacy_and_terms")

        //logger.debug(s"language is ${messages.lang}")

        val translations = ListBuffer[String]()
        messageNames.keys.foreach(k => {
                if (Messages.isDefinedAt(k)) {
                    translations += s"    ${messageNames(k)} : '${StringEscapeUtils.escapeEcmaScript(Messages(k))}'"
                }
            }
        )

        if (translations.isEmpty) {
            None
        } else {
            Some(translations.mkString(",\n"))
        }
    }
}
