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

import play.api.{Application, Configuration, Logger, Play, Plugin}

/**
 * Play plugin for the recaptcha module, hooks into the play application lifecycle.
 * 
 * @author Chris Nappin
 */
class RecaptchaPlugin(app: Application) extends Plugin {

    val logger = Logger(this.getClass())
    
    /**
     * Decide whether the plugin is enabled, by sanity checking the configuration.
     * 
     * Result is cached so it can be checked repeatedly at runtime, not just by Play on start-up.
     */
    val isEnabled = 
        isMandatoryConfigurationPresent(app.configuration) && isConfigurationValid(app.configuration)
    
    /**
     * Determine whether reCAPTCHA API version 1 is in use (if not must be API version 2, aka
     * no-captcha recaptcha).
     * 
     * Result is cached so it can be checked repeatedly at runtime.
     */
    val isApiVersion1 = getApiVersion() == Some(1)
    
    /**
     * Called first, for the plugin to decide whether it is enabled.
     * @return <code>true</code> if enabled
     */
    override def enabled(): Boolean = {
        logger.debug("enabled called")
        isEnabled
    }
    
    /**
     * Called when the client application starts up, if this plugin is enabled.
     */
    override def onStart(): Unit = {
        logger.debug("onStart called")
    } 
    
    /**
     * Called when the client application shuts down, if this plugin is enabled.
     */
    override def onStop(): Unit = {
        logger.debug("onStop called")
    }
    
    /**
     * Determines whether the mandatory configuration is present. If not a suitable error log message will be written.
     * @param configuration		The configuration to check
     * @return <code>true</code> if all present and correct
     */
    private def isMandatoryConfigurationPresent(configuration: Configuration): Boolean = {
        var mandatoryConfigurationPresent = true
        
        // keep looping so all missing items get logged, not just the first one...
        RecaptchaConfiguration.mandatoryConfiguration.foreach(key => {
            if (!configuration.keys.contains(key)) {
                logger.error(key + " not found in application configuration")
                mandatoryConfigurationPresent = false
            }
        })
        
        if (!mandatoryConfigurationPresent) {
            logger.error("Mandatory configuration missing, so recaptcha module will be disabled. " +
                    "Please check the module documentation and add the missing items to your application.conf file.")
        }
        
        return mandatoryConfigurationPresent
    }
    
    /**
     * Determines whether the configuration is valid. If not a suitable error log message will be written.
     * @param configuration		The configuration to check
     * @return <code>true</code> if all ok
     */
    private def isConfigurationValid(configuration: Configuration): Boolean = {
        var configurationValid = true
        
        // check the api version first
        var apiVersion = getApiVersion()
        if (apiVersion.isEmpty) {
            // error already logged
            configurationValid = false
        
        } else if (apiVersion.get == 1) {
            // check API version 1 configuration...
            
	        // keep going so all invalid items get logged, not just the first one...
	        RecaptchaConfiguration.booleanConfiguration.foreach(key => {
	            if (!validateBoolean(key, configuration)) {
	                configurationValid = false
	            }
	        })
	        
	        // sanity check the default language (if set) is a supported one
	        // only log as a warning since the supported languages might be out of date
	        configuration.getString(RecaptchaConfiguration.defaultLanguage).foreach(key => {
	        	if (!WidgetHelper.isSupportedLanguage(key)) {
	        	    logger.warn(s"The default language you have set ($key) is not supported by reCAPTCHA")
	        	}
	        })
        }
        
        if (!configurationValid) {
            logger.error("Configuration invalid, so recaptcha module will be disabled. " +
                    "Please check the module documentation and correct your application.conf file.")
        }
        
        return configurationValid
    }
    
    /**
     * Obtains the recaptcha API version configured.
     * @return The version number, or <code>None</code> if invalid (error will have been logged).
     */
    def getApiVersion(): Option[Int] = {
        var apiVersion = 0
        var versionString = app.configuration.getString(RecaptchaConfiguration.apiVersion).getOrElse("0")
        try {
            apiVersion = versionString.toInt
        } catch {
            case ex: NumberFormatException =>
                logger.error(s"$versionString is not a valid recaptcha API version")
                return None
        }
        if (apiVersion < 1 || apiVersion > 2) {
            logger.error(s"recaptcha API version $apiVersion is not supported")
            return None
        }
        
        return Some(apiVersion)
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

/**
 * Useful methods used by the rest of the code.
 */
object RecaptchaPlugin {
    
    /**
     * Determines whether the plugin is present and enabled.
     * @return <code>true</code> if enabled
     */
    def isEnabled(): Boolean = {
	    Play.current.plugin[RecaptchaPlugin] map { _.isEnabled } getOrElse false
    }
    
    /**
     * Determines whether API version 1 is enabled.
     * @return <code>true</code> if enabled
     */
    def isApiVersion1(): Boolean = {
	    Play.current.plugin[RecaptchaPlugin] map { _.isApiVersion1 } getOrElse false
    }
}

/**
 * Defines the configuration keys used by the module.
 */
object RecaptchaConfiguration {
    
    import scala.concurrent.duration._
    
    /** The application's recaptcha private key. */
    val privateKey = "recaptcha.privateKey"
        
    /** The application's recaptcha public key. */
    val publicKey = "recaptcha.publicKey"    
        
    /** The version of recaptcha API to use. */    
    val apiVersion = "recaptcha.apiVersion"    
        
    /** The millisecond duration to use as request timeout, when connecting to the recaptcha web API. */    
    val requestTimeout = "recaptcha.requestTimeout"
    
    /** The theme for the recaptcha widget to use (if any). */
    val theme = "recaptcha.theme"
        
    /** The default language (if any) to use if browser doesn't support any languages supported by reCAPTCHA. */    
    val defaultLanguage = "recaptcha.defaultLanguage" 
        
    /** Whether to use the secure (SSL) URL to access the verify API. */    
    val useSecureVerifyUrl = "recaptcha.useSecureVerifyUrl"  
        
    /** Whether to use the secure (SSL) URL to render the reCAPCTHA widget. */    
    val useSecureWidgetUrl = "recaptcha.useSecureWidgetUrl"     
        
    /** The mandatory configuration items that must exist for this module to work. */    
    private[recaptcha] val mandatoryConfiguration = Seq(privateKey, publicKey, apiVersion)   
    
    /** The boolean configuration items. */
    private[recaptcha] val booleanConfiguration = Seq(useSecureVerifyUrl, useSecureWidgetUrl)
}