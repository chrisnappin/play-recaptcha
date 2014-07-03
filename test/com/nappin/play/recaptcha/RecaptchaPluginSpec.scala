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

import play.api.test.FakeApplication

/**
 * Tests the <code>RecaptchaPlugin</code> class.
 * 
 * @author Chris Nappin
 */
@RunWith(classOf[JUnitRunner])
class RecaptchaPluginSpec extends Specification {
    
    "RecaptchaPlugin" should {
        
        "not be enabled if mandatory configuration missing" in {
            val invalidApplication = new FakeApplication()
            
            new RecaptchaPlugin(invalidApplication).enabled must equalTo(false)
        }
        
        "be enabled if mandatory configuration present" in {
            val validApplication = 
		        new FakeApplication(additionalConfiguration = Map(
		            RecaptchaConfiguration.privateKey -> "private-key",
		            RecaptchaConfiguration.publicKey -> "public-key"))
            
            new RecaptchaPlugin(validApplication).enabled must equalTo(true)
        }
    }    
}