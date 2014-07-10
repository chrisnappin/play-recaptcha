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

import play.api.{Application, Configuration, Logger, Plugin}

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
    val isEnabled = isMandatoryConfigurationPresent(app.configuration)
    
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
                    "Please check the module documenation and add the missing items to your application.conf file.")
        }
        
        return mandatoryConfigurationPresent
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
        
    /** The millisecond duration to use as request timeout, when connecting to the recaptcha web API. */    
    val requestTimeout = "recaptcha.requestTimeout"    
        
    /** The default request timeout to use if none is explicitly defined. */    
    val defaultRequestTimeout = 10.seconds.toMillis
    
    /** The theme for the recaptcha widget to use (if any). */
    val theme = "recaptcha.theme"
        
    /** The mandatory configuration items that must exist for this module to work. */    
    val mandatoryConfiguration = Seq(privateKey, publicKey)    
}