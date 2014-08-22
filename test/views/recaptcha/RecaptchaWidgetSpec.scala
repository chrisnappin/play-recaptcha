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

import play.api.test.{FakeApplication, FakeRequest, PlaySpecification, WithApplication}

/**
 * Tests the <code>recaptchaWidget</code> view template.
 * 
 * @author Chris Nappin
 */
@RunWith(classOf[JUnitRunner])
class RecaptchaWidgetSpec extends PlaySpecification {

    val scriptApi = "http://www.google.com/recaptcha/api/challenge"
        
    val noScriptApi = "http://www.google.com/recaptcha/api/noscript"
        
    // browser prefers french then english
    implicit val request = FakeRequest().withHeaders(("Accept-Language", "fr; q=1.0, en; q=0.5"))
    
    "recaptchaWidget" should {
        
        "render widget without error message" in new WithApplication(getApplication()) {
            val html = contentAsString(views.html.recaptcha.recaptchaWidget())
            
            // no error passed to recaptcha 
            html must contain(s"$scriptApi?k=public-key")
            html must contain(s"$noScriptApi?k=public-key")
            
            // fr lang should be set
            html must contain("lang : 'fr'")
            
            // no theme or tabindex
            html must not contain("theme : ")
            html must not contain("tabindex : ")
        }
        
        "render widget with error message" in new WithApplication(getApplication()) {
            val html = contentAsString(views.html.recaptcha.recaptchaWidget(error = Some("my-error-key")))
            
            // error passed to recaptcha 
            html must contain(s"$scriptApi?k=public-key&error=my-error-key")
            html must contain(s"$noScriptApi?k=public-key&error=my-error-key")
            
            // fr lang should be set
            html must contain("lang : 'fr'")
            
            // no theme or tabindex
            html must not contain("theme : ")
            html must not contain("tabindex : ")
        }
        
        "render widget with theme" in new WithApplication(getApplication(Some("my-theme"))) {
            val html = contentAsString(views.html.recaptcha.recaptchaWidget())
                        
            // no error passed to recaptcha 
            html must contain(s"$scriptApi?k=public-key")
            html must contain(s"$noScriptApi?k=public-key")
            
            // fr lang should be set with comma
            html must contain("lang : 'fr',")
            
            // must have theme set 
            html must contain("theme : 'my-theme'")
            
            // no tabindex
            html must not contain("tabindex : ")
        }
        
        "render widget with tabindex" in new WithApplication(getApplication()) {
            val html = contentAsString(views.html.recaptcha.recaptchaWidget(tabindex = Some(42)))
            
            // no error passed to recaptcha
            html must contain(s"$scriptApi?k=public-key")
            html must contain(s"$noScriptApi?k=public-key")
            
            // fr lang should be set with comma
            html must contain("lang : 'fr',")
            
            // no theme
            html must not contain("theme : ")
            
            // must have tabindex set
            html must contain("tabindex : 42")
        }
        
        "render widget with theme and tabindex" in new WithApplication(getApplication(Some("my-theme"))) {
            val html = contentAsString(views.html.recaptcha.recaptchaWidget(tabindex = Some(42)))
            
            // no error passed to recaptcha
            html must contain(s"$scriptApi?k=public-key")
            html must contain(s"$noScriptApi?k=public-key")
            
            // must have lang, theme, tabindex and commas
            html must contain("lang : 'fr',")
            html must contain("theme : 'my-theme',")
            html must contain("tabindex : 42")
        }
        
        "render widget with error message, theme and tabindex" in new WithApplication(getApplication(Some("my-theme"))) {
            val html = contentAsString(views.html.recaptcha.recaptchaWidget(
                    error = Some("my-error-key"), tabindex = Some(42)))
            
            // error passed to recaptcha 
            html must contain(s"$scriptApi?k=public-key&error=my-error-key")
            html must contain(s"$noScriptApi?k=public-key&error=my-error-key")
            
            // must have lang, theme, tabindex and commas
            html must contain("lang : 'fr',")
            html must contain("theme : 'my-theme',")
            html must contain("tabindex : 42")
        }
        
        "render widget with error message, theme and tabindex (en)" in new WithApplication(getApplication(Some("my-theme"))) {
            // browser prefers english then french
            implicit val request = FakeRequest().withHeaders(("Accept-Language", "en; q=1.0, fr; q=0.5"))
            
            val html = contentAsString(views.html.recaptcha.recaptchaWidget(
                    error = Some("my-error-key"), tabindex = Some(42)))
            
            // error passed to recaptcha 
            html must contain(s"$scriptApi?k=public-key&error=my-error-key")
            html must contain(s"$noScriptApi?k=public-key&error=my-error-key")
            
            // must have lang, theme, tabindex and commas
            html must contain("lang : 'en',")
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