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

import play.api.Logger
import play.api.i18n.{Lang, Messages}

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
     * Returns the configured public key.
     * @return The public key
     */
    def publicKey: String = settings.publicKey

    /**
     * Returns the configured theme, or "light" if none defined
     * @return The theme to use
     */
    def captchaTheme: String = settings.theme.getOrElse("light")


    /**
     * Returns the configured captcha type, or the default if none defined
     * @return The type to use
     */
    def captchaType: String = settings.captchaType

    /**
     * Returns the configured captcha size, or the default if none defined
     * @return The size to use
     */
    def captchaSize: String = settings.captchaSize


    /**
     * Returns the widget script URL, with parameters (if applicable).
     * @param error			The error code (if any)
     * @param messages		The current i18n messages
     * @return The URL
     */
    def widgetScriptUrl(error: Option[String] = None)(implicit messages: Messages): String = {
        settings.widgetScriptUrl + (
                if (settings.languageMode == "force") {
                    "?hl=" + settings.forceLanguage.get
                } else if (settings.languageMode == "play") {
                    "?hl=" + mapToV2Language(messages.lang)
                } else {
                    ""
                }
            )
    }

    /**
     * Returns the widget no-script URL, with public key and error code (if any).
     * @param error			The error code (if any)
     * @return The URL
     */
    def widgetNoScriptUrl(error: Option[String] = None): String = {
        // API v2 only includes public key
        settings.widgetNoScriptUrl + "?k=" + publicKey
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
}
