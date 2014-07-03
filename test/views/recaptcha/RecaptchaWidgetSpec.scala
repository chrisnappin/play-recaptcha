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
package views.recaptcha

import com.nappin.play.recaptcha.RecaptchaConfiguration

import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith

import play.api.test.{FakeApplication, PlaySpecification, WithApplication}

/**
 * Tests the <code>recaptchaWidget</code> view template.
 * 
 * @author Chris Nappin
 */
@RunWith(classOf[JUnitRunner])
class RecaptchaWidgetSpec extends PlaySpecification {

    val scriptApi = "http://www.google.com/recaptcha/api/challenge"
        
    val noScriptApi = "http://www.google.com/recaptcha/api/noscript"
        
    val validApplication = 
        new FakeApplication(additionalConfiguration = Map(
                RecaptchaConfiguration.privateKey -> "private-key",
                RecaptchaConfiguration.publicKey -> "public-key"))
    
    "recaptchaWidget" should {
        
        "render widget without error message if none specified" in new WithApplication(validApplication) {
            val html = views.html.recaptcha.recaptchaWidget(None)
            
            contentAsString(html) must contain(s"$scriptApi?k=public-key")
            contentAsString(html) must contain(s"$noScriptApi?k=public-key")
        }
        
        "render widget with error message if specified" in new WithApplication(validApplication) {
            val html = views.html.recaptcha.recaptchaWidget(Some("my-error-key"))
            
            contentAsString(html) must contain(s"$scriptApi?k=public-key&error=my-error-key")
            contentAsString(html) must contain(s"$noScriptApi?k=public-key&error=my-error-key")
        }
    }
}