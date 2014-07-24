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
import play.api.i18n.Lang
import play.api.mvc.{AnyContent, Request}

/**
 * Helper functionality for the <code>recaptchaWidget</code> view template.
 * 
 * @author Chris Nappin
 */
object WidgetHelper {

    /**
     * Returns the most preferred language (as extracted from the <code>Accept-Language</code> HTTP header in the 
     * request, if any) that is supported by reCAPTCHA. If no supported language is found, returns the default
     * language from configuration (if any), otherwise English ("en").
     * @param request		The web request
     * @return The language code
     */
    def getPreferredLanguage()(implicit request: Request[AnyContent]): String = {
        request.acceptLanguages.find(l => isSupportedLanguage(l.language))
        	.fold(getDefaultLanguage()) // if no supported language found
        		{lang => lang.language} // if a supported language was found
    }
    
    /**
     * Determines whether the specified language code is supported by reCAPTCHA.
     * @param languageCode		The language code
     * @return <code>true</code> if supported
     */
    def isSupportedLanguage(languageCode: String): Boolean = {
        supportedCodes.contains(languageCode)
    }
    
    /**
     * Get the default language setting (if defined) or the default for this setting.
     * @return The language code
     */
    private def getDefaultLanguage(): String = {
        current.configuration.getString(RecaptchaConfiguration.defaultLanguage).getOrElse("en")
    }
    
    /** As taken from Google reCAPTCHA documentation, 23/07/2014. This needs to be kept up to date. */
    private val supportedCodes = Seq( "en", "nl", "fr", "de", "pt", "ru", "es", "tr" ) 
}