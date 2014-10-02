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

import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith

import play.api.test.{FakeApplication, PlaySpecification, WithApplication}

/**
 * Tests the <code>RecaptchaUrls</code> class.
 * 
 * @author Chris Nappin
 */
@RunWith(classOf[JUnitRunner])
class RecaptchaUrlsSpec extends PlaySpecification {
    
    val noConfig = new FakeApplication(additionalConfiguration = Map(
                RecaptchaConfiguration.privateKey -> "private-key",
                RecaptchaConfiguration.publicKey -> "public-key"))
    
    val insecureConfig = new FakeApplication(additionalConfiguration = Map(
                RecaptchaConfiguration.privateKey -> "private-key",
                RecaptchaConfiguration.publicKey -> "public-key",
                RecaptchaConfiguration.useSecureVerifyUrl -> "false",
                RecaptchaConfiguration.useSecureWidgetUrl -> "false"))
    
    val altInsecureConfig = new FakeApplication(additionalConfiguration = Map(
                RecaptchaConfiguration.privateKey -> "private-key",
                RecaptchaConfiguration.publicKey -> "public-key",
                RecaptchaConfiguration.useSecureVerifyUrl -> "no",
                RecaptchaConfiguration.useSecureWidgetUrl -> "no"))
    
    val secureConfig = new FakeApplication(additionalConfiguration = Map(
                RecaptchaConfiguration.privateKey -> "private-key",
                RecaptchaConfiguration.publicKey -> "public-key",
                RecaptchaConfiguration.useSecureVerifyUrl -> "true",
                RecaptchaConfiguration.useSecureWidgetUrl -> "true"))
    
    val altSecureConfig = new FakeApplication(additionalConfiguration = Map(
                RecaptchaConfiguration.privateKey -> "private-key",
                RecaptchaConfiguration.publicKey -> "public-key",
                RecaptchaConfiguration.useSecureVerifyUrl -> "yes",
                RecaptchaConfiguration.useSecureWidgetUrl -> "yes")) 
    
    "getVerifyUrl" should {
        
        "use insecure url if no configuration present" in new WithApplication(noConfig) {
            
            RecaptchaUrls.getVerifyUrl must equalTo("http://www.google.com/recaptcha/api/verify")
        }
        
        "use insecure url if explicitly set to false" in new WithApplication(insecureConfig) {
            
            RecaptchaUrls.getVerifyUrl must equalTo("http://www.google.com/recaptcha/api/verify")
        }
        
        "use insecure url if explicitly set to no" in new WithApplication(altInsecureConfig) {
            
            RecaptchaUrls.getVerifyUrl must equalTo("http://www.google.com/recaptcha/api/verify")
        }
        
        "use secure url if explicitly set to true" in new WithApplication(secureConfig) {
            
            RecaptchaUrls.getVerifyUrl must equalTo("https://www.google.com/recaptcha/api/verify")
        }
        
        "use secure url if explicitly set to yes" in new WithApplication(altSecureConfig) {
            
            RecaptchaUrls.getVerifyUrl must equalTo("https://www.google.com/recaptcha/api/verify")
        }
    } 
    
    "getWidgetScriptUrl" should {
        
        "use insecure url if no configuration present" in new WithApplication(noConfig) {
            
            RecaptchaUrls.getWidgetScriptUrl must equalTo("http://www.google.com/recaptcha/api/challenge")
        }
        
        "use insecure url if explicitly set to false" in new WithApplication(insecureConfig) {
            
            RecaptchaUrls.getWidgetScriptUrl must equalTo("http://www.google.com/recaptcha/api/challenge")
        }
        
        "use insecure url if explicitly set to no" in new WithApplication(altInsecureConfig) {
            
            RecaptchaUrls.getWidgetScriptUrl must equalTo("http://www.google.com/recaptcha/api/challenge")
        }
        
        "use secure url if explicitly set to true" in new WithApplication(secureConfig) {
            
            RecaptchaUrls.getWidgetScriptUrl must equalTo("https://www.google.com/recaptcha/api/challenge")
        }
        
        "use secure url if explicitly set to yes" in new WithApplication(altSecureConfig) {
            
            RecaptchaUrls.getWidgetScriptUrl must equalTo("https://www.google.com/recaptcha/api/challenge")
        }
    }
    
    "getWidgetNoScriptUrl" should {
        
        "use insecure url if no configuration present" in new WithApplication(noConfig) {
            
            RecaptchaUrls.getWidgetNoScriptUrl must equalTo("http://www.google.com/recaptcha/api/noscript")
        }
        
        "use insecure url if explicitly set to false" in new WithApplication(insecureConfig) {
            
            RecaptchaUrls.getWidgetNoScriptUrl must equalTo("http://www.google.com/recaptcha/api/noscript")
        }
        
        "use insecure url if explicitly set to no" in new WithApplication(altInsecureConfig) {
            
            RecaptchaUrls.getWidgetNoScriptUrl must equalTo("http://www.google.com/recaptcha/api/noscript")
        }
        
        "use secure url if explicitly set to true" in new WithApplication(secureConfig) {
            
            RecaptchaUrls.getWidgetNoScriptUrl must equalTo("https://www.google.com/recaptcha/api/noscript")
        }
        
        "use secure url if explicitly set to yes" in new WithApplication(altSecureConfig) {
            
            RecaptchaUrls.getWidgetNoScriptUrl must equalTo("https://www.google.com/recaptcha/api/noscript")
        }
    }
}