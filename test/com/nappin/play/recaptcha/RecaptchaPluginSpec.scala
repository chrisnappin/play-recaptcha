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

import play.api.Play
import play.api.test.{FakeApplication, WithApplication}

/**
 * Tests the <code>RecaptchaPlugin</code> class.
 * 
 * @author Chris Nappin
 */
@RunWith(classOf[JUnitRunner])
class RecaptchaPluginSpec extends Specification {
    
    val plugins = Seq("com.nappin.play.recaptcha.RecaptchaPlugin")
    
    "RecaptchaPlugin (api v1)" should {
        
        "not be enabled if mandatory configuration missing" in 
                new WithApplication(new FakeApplication(additionalPlugins = plugins)) {
            new RecaptchaPlugin(Play.current).isEnabled must equalTo(false)
            RecaptchaPlugin.isEnabled must equalTo(false)
        }
        
        "be enabled if mandatory configuration present" in
                new WithApplication(new FakeApplication(
		            additionalPlugins = plugins,
		            additionalConfiguration = Map(
		                RecaptchaConfiguration.privateKey -> "private-key",
		                RecaptchaConfiguration.publicKey -> "public-key",
		                RecaptchaConfiguration.apiVersion -> "1"))) {   
            new RecaptchaPlugin(Play.current).isEnabled must equalTo(true)
            RecaptchaPlugin.isEnabled must equalTo(true)
        }
        
        "be enabled if booleans are true or false" in
                new WithApplication(new FakeApplication(
		            additionalPlugins = plugins,
                    additionalConfiguration = Map(
			            RecaptchaConfiguration.privateKey -> "private-key",
			            RecaptchaConfiguration.publicKey -> "public-key",
			            RecaptchaConfiguration.apiVersion -> "1",
			            RecaptchaConfiguration.useSecureVerifyUrl -> "true",
			            RecaptchaConfiguration.useSecureWidgetUrl -> "false"))) {
            new RecaptchaPlugin(Play.current).isEnabled must equalTo(true)
            RecaptchaPlugin.isEnabled must equalTo(true)
        }
        
        "be enabled if booleans are yes or no" in
                new WithApplication(new FakeApplication(
                    additionalPlugins = plugins,
                    additionalConfiguration = Map(
			            RecaptchaConfiguration.privateKey -> "private-key",
			            RecaptchaConfiguration.publicKey -> "public-key",
			            RecaptchaConfiguration.apiVersion -> "1",
			            RecaptchaConfiguration.useSecureVerifyUrl -> "yes",
			            RecaptchaConfiguration.useSecureWidgetUrl -> "no"))) {            
            new RecaptchaPlugin(Play.current).isEnabled must equalTo(true)
            RecaptchaPlugin.isEnabled must equalTo(true)
        }
        
        "not be enabled if useSecureVerifyUrl invalid" in
                new WithApplication(new FakeApplication(
                    additionalPlugins = plugins,
                    additionalConfiguration = Map(
			            RecaptchaConfiguration.privateKey -> "private-key",
			            RecaptchaConfiguration.publicKey -> "public-key",
			            RecaptchaConfiguration.apiVersion -> "1",
			            RecaptchaConfiguration.useSecureVerifyUrl -> "wibble"))) {            
            new RecaptchaPlugin(Play.current).isEnabled must equalTo(false)
            RecaptchaPlugin.isEnabled must equalTo(false)
        }
        
        "not be enabled if useSecureWidgetUrl invalid" in
                new WithApplication(new FakeApplication(
                    additionalPlugins = plugins,
                    additionalConfiguration = Map(
			            RecaptchaConfiguration.privateKey -> "private-key",
			            RecaptchaConfiguration.publicKey -> "public-key",
			            RecaptchaConfiguration.apiVersion -> "1",
			            RecaptchaConfiguration.useSecureWidgetUrl -> "wibble"))) {            
            new RecaptchaPlugin(Play.current).isEnabled must equalTo(false)
            RecaptchaPlugin.isEnabled must equalTo(false)
        }
        
        "be enabled if language unsupported" in
                new WithApplication(new FakeApplication(
                    additionalPlugins = plugins,
                    additionalConfiguration = Map(
			            RecaptchaConfiguration.privateKey -> "private-key",
			            RecaptchaConfiguration.publicKey -> "public-key",
			            RecaptchaConfiguration.apiVersion -> "1",
			            RecaptchaConfiguration.defaultLanguage -> "zz"))) {            
            new RecaptchaPlugin(Play.current).isEnabled must equalTo(true)
            RecaptchaPlugin.isEnabled must equalTo(true)
        }
        
        "api version 1 enabled if valid" in
                new WithApplication(new FakeApplication(
                    additionalPlugins = plugins,    
                    additionalConfiguration = Map(
			            RecaptchaConfiguration.privateKey -> "private-key",
			            RecaptchaConfiguration.publicKey -> "public-key",
			            RecaptchaConfiguration.apiVersion -> "1"))) {            
            new RecaptchaPlugin(Play.current).isApiVersion1 must equalTo(true)
            RecaptchaPlugin.isApiVersion1 must equalTo(true)
        }
    }    
    
    "RecaptchaPlugin (api v2)" should {
        
        "not be enabled if mandatory configuration missing" in
                new WithApplication(new FakeApplication(additionalPlugins = plugins)) {            
            new RecaptchaPlugin(Play.current).isEnabled must equalTo(false)
            RecaptchaPlugin.isEnabled must equalTo(false)
        }
        
        "be enabled if mandatory configuration present" in
                new WithApplication(new FakeApplication(
                    additionalPlugins = plugins,
                    additionalConfiguration = Map(
			            RecaptchaConfiguration.privateKey -> "private-key",
			            RecaptchaConfiguration.publicKey -> "public-key",
			            RecaptchaConfiguration.apiVersion -> "2"))) {            
            new RecaptchaPlugin(Play.current).isEnabled must equalTo(true)
            RecaptchaPlugin.isEnabled must equalTo(true)
        }
        
        "api version 2 enabled if valid" in
                new WithApplication(new FakeApplication(
                    additionalPlugins = plugins,
                    additionalConfiguration = Map(
			            RecaptchaConfiguration.privateKey -> "private-key",
			            RecaptchaConfiguration.publicKey -> "public-key",
			            RecaptchaConfiguration.apiVersion -> "2"))) {            
            new RecaptchaPlugin(Play.current).isApiVersion1 must equalTo(false)
            RecaptchaPlugin.isApiVersion1 must equalTo(false)
        }
    }
    
    "RecaptchaPlugin (invalid api version)" should {
        
        "not be enabled if api version invalid" in
                new WithApplication(new FakeApplication(
                    additionalPlugins = plugins,
                    additionalConfiguration = Map(
			            RecaptchaConfiguration.privateKey -> "private-key",
			            RecaptchaConfiguration.publicKey -> "public-key",
			            RecaptchaConfiguration.apiVersion -> "wibble"))) {            
            new RecaptchaPlugin(Play.current).isEnabled must equalTo(false)
            RecaptchaPlugin.isEnabled must equalTo(false)
        }
        
        "not be enabled if api version unsupported" in
                new WithApplication(new FakeApplication(
                    additionalPlugins = plugins,
                    additionalConfiguration = Map(
			            RecaptchaConfiguration.privateKey -> "private-key",
			            RecaptchaConfiguration.publicKey -> "public-key",
			            RecaptchaConfiguration.apiVersion -> "3"))) {            
            new RecaptchaPlugin(Play.current).isEnabled must equalTo(false)
            RecaptchaPlugin.isEnabled must equalTo(false)
        }
        
        "api version 1 not enabled if api version unsupported" in
                new WithApplication(new FakeApplication(
                    additionalPlugins = plugins,
                    additionalConfiguration = Map(
			            RecaptchaConfiguration.privateKey -> "private-key",
			            RecaptchaConfiguration.publicKey -> "public-key",
			            RecaptchaConfiguration.apiVersion -> "3"))) {            
            new RecaptchaPlugin(Play.current).isApiVersion1 must equalTo(false)
            RecaptchaPlugin.isApiVersion1 must equalTo(false)
        }
    }
}