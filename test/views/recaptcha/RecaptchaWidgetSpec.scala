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
    
    "recaptchaWidget" should {
        
        "render widget without error message" in new WithApplication(getApplication()) {
            val html = contentAsString(views.html.recaptcha.recaptchaWidget())
            
            // no error passed to recaptcha 
            html must contain(s"$scriptApi?k=public-key")
            html must contain(s"$noScriptApi?k=public-key")
            
            // no options needed
            html must not contain("RecaptchaOptions")
        }
        
        "render widget with error message" in new WithApplication(getApplication()) {
            val html = contentAsString(views.html.recaptcha.recaptchaWidget(error = Some("my-error-key")))
            
            // error passed to recaptcha 
            html must contain(s"$scriptApi?k=public-key&error=my-error-key")
            html must contain(s"$noScriptApi?k=public-key&error=my-error-key")
            
            // no options needed
            html must not contain("RecaptchaOptions")
        }
        
        "render widget with theme" in new WithApplication(getApplication(Some("my-theme"))) {
            val html = contentAsString(views.html.recaptcha.recaptchaWidget())
                        
            // no error passed to recaptcha 
            html must contain(s"$scriptApi?k=public-key")
            html must contain(s"$noScriptApi?k=public-key")
            
            // must have options with theme 
            html must contain("RecaptchaOptions")
            html must contain("theme : 'my-theme'")
        }
        
        "render widget with tabindex" in new WithApplication(getApplication()) {
            val html = contentAsString(views.html.recaptcha.recaptchaWidget(tabindex = Some(42)))
            
            // no error passed to recaptcha
            html must contain(s"$scriptApi?k=public-key")
            html must contain(s"$noScriptApi?k=public-key")
            
            // must have options with tabindex 
            html must contain("RecaptchaOptions")
            html must contain("tabindex : 42")
        }
        
        "render widget with theme and tabindex" in new WithApplication(getApplication(Some("my-theme"))) {
            val html = contentAsString(views.html.recaptcha.recaptchaWidget(tabindex = Some(42)))
            
            // no error passed to recaptcha
            html must contain(s"$scriptApi?k=public-key")
            html must contain(s"$noScriptApi?k=public-key")
            
            // must have options with theme, comma and tabindex 
            html must contain("RecaptchaOptions")
            html must contain("theme : 'my-theme',")
            html must contain("tabindex : 42")
        }
        
        "render widget with error message, theme and tabindex" in new WithApplication(getApplication(Some("my-theme"))) {
            val html = contentAsString(views.html.recaptcha.recaptchaWidget(
                    error = Some("my-error-key"), tabindex = Some(42)))
            
            // error passed to recaptcha 
            html must contain(s"$scriptApi?k=public-key&error=my-error-key")
            html must contain(s"$noScriptApi?k=public-key&error=my-error-key")
            
            // must have options with theme, comma and tabindex 
            html must contain("RecaptchaOptions")
            html must contain("theme : 'my-theme',")
            html must contain("tabindex : 42")
        }
    }
    
    /**
     * Get the fake application context.
     * @param theme		The configured theme (if any)
     * @return The application
     */
    private def getApplication(theme: Option[String] = None): FakeApplication = {
        var config = Map(
                RecaptchaConfiguration.privateKey -> "private-key",
                RecaptchaConfiguration.publicKey -> "public-key")
                
        if (theme.isDefined) {
            config += RecaptchaConfiguration.theme -> theme.get
        }        
        
        new FakeApplication(additionalConfiguration = config)
    }
}