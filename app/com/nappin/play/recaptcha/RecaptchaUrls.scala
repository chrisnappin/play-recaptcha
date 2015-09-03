/*
 * Copyright 2014 Chris Nappin
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

import play.api.Play.current

/**
 * Handles secure and in-secure variants of all reCAPTCHA widget, script and API URLs.
 *
 * @author @author Chris Nappin
 */
object RecaptchaUrls {

    /**
     * Get the URL (secure or insecure) for the verify API.
     * @return The URL
     */
    def getVerifyUrl(): String = {
        if (RecaptchaModule.isApiVersion1) {
            getPrefix(RecaptchaConfiguration.useSecureVerifyUrl) +
                "://www.google.com/recaptcha/api/verify"

        } else {
            "https://www.google.com/recaptcha/api/siteverify"
        }
    }

    /**
     * Get the URL (secure or insecure if v1, always secure if v2) for the widget (script).
     * @return The URL
     */
    def getWidgetScriptUrl(): String = {
        if (RecaptchaModule.isApiVersion1) {
            getPrefix(RecaptchaConfiguration.useSecureWidgetUrl) +
                "://www.google.com/recaptcha/api/challenge"

        } else {
            "https://www.google.com/recaptcha/api.js"
        }
    }

    /**
     * Get the URL (secure or insecure if v1, always secure if v2) for the widget (no script).
     * @return The URL
     */
    def getWidgetNoScriptUrl(): String = {
        if (RecaptchaModule.isApiVersion1) {
            getPrefix(RecaptchaConfiguration.useSecureWidgetUrl) +
                "://www.google.com/recaptcha/api/noscript"

        } else {
            "https://www.google.com/recaptcha/api/fallback"
        }
    }

    /**
     * Get the URL prefix (secure or insecure) as specified by the configuration setting,
     * defaulting to insecure if configuration not set.
     * @param setting		The configuration setting key to check
     * @return The prefix
     */
    private def getPrefix(setting: String): String = {
        val isSecure = current.configuration.getBoolean(setting).getOrElse(false)
        if (isSecure) "https" else "http"
    }
}
