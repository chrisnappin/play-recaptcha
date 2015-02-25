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

import org.apache.commons.lang3.StringEscapeUtils

import play.api.Logger
import play.api.Play.current
import play.api.i18n.{Lang, Messages}
import play.api.mvc.{AnyContent, Request}

import scala.collection.mutable.ListBuffer

/**
 * Helper functionality for the <code>recaptchaWidget</code> view template.
 * 
 * @author Chris Nappin
 */
object WidgetHelper {

    val logger = Logger(this.getClass())
    
    /**
     * Determines whether API version 1 has been configured.
     * @return <code>true</code> if configured
     */
    def isApiVersion1(): Boolean = {
        RecaptchaPlugin.isApiVersion1
    }
    
    /**
     * Returns the configured public key.
     * @return The public key
     */
    def getPublicKey(): String = {
        current.configuration.getString(RecaptchaConfiguration.publicKey).getOrElse("")
    }
    
    /**
     * Returns the configured theme, or "light" if none defined
     * @return The theme to use
     */
    def getV2Theme(): String = {
        current.configuration.getString(RecaptchaConfiguration.theme).getOrElse("light")
    }
    
    /**
     * Returns the configured captcha type, or "image" if none defined
     * @return The type to use
     */
    def getV2Type(): String = {
        current.configuration.getString(RecaptchaConfiguration.captchaType).getOrElse("image")
    }
    
    /**
     * Returns the widget script URL, with parameters (if applicable).
     * @param error			The error code (if any)
     * @return The URL
     */
    def getWidgetScriptUrl(error: Option[String] = None): String = {
        if (isApiVersion1) {
            // API v1 includes public key and error code (if any) 
	        val errorSuffix = error.map("&error=" + _).getOrElse("")
	        val publicKey = current.configuration.getString(RecaptchaConfiguration.publicKey)
	                .map("?k=" + _).getOrElse("")
	        
	        RecaptchaUrls.getWidgetScriptUrl + publicKey + errorSuffix
	        
        } else {
            // API v2 doesn't include any URL parameters
            RecaptchaUrls.getWidgetScriptUrl
        }
    }
    
    /**
     * Returns the widget no-script URL, with public key and error code (if any).
     * @param error			The error code (if any)
     * @return The URL
     */
    def getWidgetNoScriptUrl(error: Option[String] = None): String = {
        val publicKey = current.configuration.getString(RecaptchaConfiguration.publicKey)
                .map("?k=" + _).getOrElse("")
        
        if (isApiVersion1) {
            // API v1 includes public key and error code (if any)
            val errorSuffix = error.map("&error=" + _).getOrElse("")
            
            RecaptchaUrls.getWidgetNoScriptUrl + publicKey + errorSuffix
            
        } else {
            // API v2 only includes public key
            RecaptchaUrls.getWidgetNoScriptUrl + publicKey
        }
    }
    
    /**
     * Returns the <code>RecaptchaOptions</code> JavaScript declaration, with the appropriate customisation options.
     * @param tabindex		The tabindex (if any)
     * @param request		The web request
     * @param lang			The current language
     * @return The JavaScript code
     */
    def getRecaptchaOptions(tabindex: Option[Int])(implicit request: Request[AnyContent], lang: Lang): String = {
        val theme = current.configuration.getString(RecaptchaConfiguration.theme)
        
        new StringBuffer("var RecaptchaOptions = {\n")
            .append(s"  lang : '${getPreferredLanguage()}'")
            .append(theme.fold(""){t => s",\n  theme : '$t'"})
            .append(tabindex.fold(""){t => s",\n  tabindex : $t"})
            .append(getCustomTranslations().fold(""){t => s",\n  custom_translations : {\n$t\n  }"})
            .append("\n};").toString()
    }
    
    /**
     * Returns the most preferred language (as extracted from the <code>Accept-Language</code> HTTP header in the 
     * request, if any) that is supported by reCAPTCHA. If no supported language is found, returns the default
     * language from configuration (if any), otherwise English ("en").
     * @param request		The web request
     * @return The language code
     */
    private[recaptcha] def getPreferredLanguage()(implicit request: Request[AnyContent]): String = {
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
    
    /**
     * Get the custom translations (if any) formatted as a JavaScript dictionary of escaped strings.
     * @param lang		The current language
     * @return The custom translations (if any)
     */
    private def getCustomTranslations()(implicit lang: Lang): Option[String] = {
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
        
        logger.debug(s"language is $lang")
        
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
    
    /** As taken from Google reCAPTCHA documentation, 23/07/2014. This needs to be kept up to date. */
    private val supportedCodes = Seq( "en", "nl", "fr", "de", "pt", "ru", "es", "tr" ) 
}