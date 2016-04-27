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

import com.nappin.play.recaptcha.{RecaptchaConfiguration, WidgetHelper}
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import play.api.i18n.MessagesApi
import play.api.test.{FakeApplication, FakeRequest, PlaySpecification, WithApplication}
import play.api.{Application}

/**
 * Tests the <code>recaptchaWidget</code> view template.
 *
 * @author Chris Nappin
 */
@RunWith(classOf[JUnitRunner])
class RecaptchaWidgetSpec extends PlaySpecification {

    def widgetHelper(implicit application: Application) = application.injector.instanceOf[WidgetHelper]
    def messages(implicit application: Application) = application.injector.instanceOf[MessagesApi].preferred(request)

    val scriptApi = "http://www.google.com/recaptcha/api/challenge"

    val noScriptApi = "http://www.google.com/recaptcha/api/noscript"

    // browser prefers french then english
    val request = FakeRequest().withHeaders(("Accept-Language", "fr; q=1.0, en; q=0.5"))

    "recaptchaWidget (v1)" should {

        "render v1 widget without error message" in new WithApplication(getApplication(1)) {

            val html = contentAsString(views.html.recaptcha.recaptchaWidget()(request, messages, widgetHelper))

            // no error passed to recaptcha
            html must contain(s"$scriptApi?k=public-key")
            html must contain(s"$noScriptApi?k=public-key")

            // fr lang should be set
            html must contain("lang : 'fr'")

            // no theme or tabindex
            html must not contain("theme : ")
            html must not contain("tabindex : ")
        }

        "render v1 widget with error message" in new WithApplication(getApplication(1)) {

            val html = contentAsString(
                    views.html.recaptcha.recaptchaWidget(error = Some("my-error-key"))(
                        request, messages, widgetHelper))

            // error passed to recaptcha
            html must contain(s"$scriptApi?k=public-key&error=my-error-key")
            html must contain(s"$noScriptApi?k=public-key&error=my-error-key")

            // fr lang should be set
            html must contain("lang : 'fr'")

            // no theme or tabindex
            html must not contain("theme : ")
            html must not contain("tabindex : ")
        }

        "render v1 widget with theme" in new WithApplication(getApplication(1, Some("my-theme"))) {

            val html = contentAsString(views.html.recaptcha.recaptchaWidget()(request, messages, widgetHelper))

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

        "render v1 widget with tabindex" in new WithApplication(getApplication(1)) {

            val html = contentAsString(views.html.recaptcha.recaptchaWidget(tabindex = Some(42))(
                    request, messages, widgetHelper))

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

        "render v1 widget with theme and tabindex" in
                new WithApplication(getApplication(1, Some("my-theme"))) {

            val html = contentAsString(views.html.recaptcha.recaptchaWidget(tabindex = Some(42))(
                    request, messages, widgetHelper))

            // no error passed to recaptcha
            html must contain(s"$scriptApi?k=public-key")
            html must contain(s"$noScriptApi?k=public-key")

            // must have lang, theme, tabindex and commas
            html must contain("lang : 'fr',")
            html must contain("theme : 'my-theme',")
            html must contain("tabindex : 42")
        }

        "render v1 widget with error message, theme and tabindex" in
                new WithApplication(getApplication(1, Some("my-theme"))) {

            val html = contentAsString(views.html.recaptcha.recaptchaWidget(
                    error = Some("my-error-key"), tabindex = Some(42))(request, messages, widgetHelper))

            // error passed to recaptcha
            html must contain(s"$scriptApi?k=public-key&error=my-error-key")
            html must contain(s"$noScriptApi?k=public-key&error=my-error-key")

            // must have lang, theme, tabindex and commas
            html must contain("lang : 'fr',")
            html must contain("theme : 'my-theme',")
            html must contain("tabindex : 42")
        }

        "render v1 widget with error message, theme and tabindex (en)" in
                new WithApplication(getApplication(1, Some("my-theme"))) {
            // browser prefers english then french
            val request = FakeRequest().withHeaders(("Accept-Language", "en; q=1.0, fr; q=0.5"))

            val html = contentAsString(views.html.recaptcha.recaptchaWidget(
                    error = Some("my-error-key"), tabindex = Some(42))(request, messages, widgetHelper))

            // error passed to recaptcha
            html must contain(s"$scriptApi?k=public-key&error=my-error-key")
            html must contain(s"$noScriptApi?k=public-key&error=my-error-key")

            // must have lang, theme, tabindex and commas
            html must contain("lang : 'en',")
            html must contain("theme : 'my-theme',")
            html must contain("tabindex : 42")
        }
    }

    "recaptchaWidget (v2)" should {

        "render v2 widget with noscript block" in new WithApplication(getApplication(2)) {
            val html = contentAsString(views.html.recaptcha.recaptchaWidget()(request, messages, widgetHelper))

            // includes script
            html must contain("<script")
            html must contain("g-recaptcha")
            html must contain("data-sitekey=\"public-key\"")

            // default theme and type
            html must contain("data-theme=\"light\"")
            html must contain("data-type=\"image\"")

            // includes noscript block
            html must contain("<noscript")
            html must contain("g-recaptcha-response")
            html must contain("fallback?k=public-key")
        }

        "render v2 widget without noscript" in new WithApplication(getApplication(2)) {
            val html = contentAsString(views.html.recaptcha.recaptchaWidget(includeNoScript = false)(
                    request, messages, widgetHelper))

            // includes script
            html must contain("<script")
            html must contain("g-recaptcha")
            html must contain("data-sitekey=\"public-key\"")

            // default theme and type
            html must contain("data-theme=\"light\"")
            html must contain("data-type=\"image\"")

            // doesn't include noscript block
            html must not contain("<noscript")
            html must not contain("g-recaptcha-response")
            html must not contain("fallback?k=public-key")
        }

        "render v2 widget with theme" in new WithApplication(getApplication(2, Some("dark"))) {
            val html = contentAsString(views.html.recaptcha.recaptchaWidget()(request, messages, widgetHelper))

            // includes script
            html must contain("<script")
            html must contain("g-recaptcha")
            html must contain("data-sitekey=\"public-key\"")

            // explicit theme, default type
            html must contain("data-theme=\"dark\"")
            html must contain("data-type=\"image\"")

            // includes noscript block
            html must contain("<noscript")
            html must contain("g-recaptcha-response")
            html must contain("fallback?k=public-key")
        }

        "render v2 widget with type" in new WithApplication(getApplication(2, None, Some("audio"))) {
            val html = contentAsString(views.html.recaptcha.recaptchaWidget()(request, messages, widgetHelper))

            // includes script
            html must contain("<script")
            html must contain("g-recaptcha")
            html must contain("data-sitekey=\"public-key\"")

            // default theme, explicit type
            html must contain("data-theme=\"light\"")
            html must contain("data-type=\"audio\"")

            // includes noscript block
            html must contain("<noscript")
            html must contain("g-recaptcha-response")
            html must contain("fallback?k=public-key")
        }
    }

    /**
     * Get the fake application context.
     * @param version		The API version to use
     * @param theme			The configured theme (if any)
     * @param captchaType	The captcha type (if any)
     * @return The application
     */
    private def getApplication(version: Int, theme: Option[String] = None,
            captchaType: Option[String] = None): Application = {
        var config = Map(
                RecaptchaConfiguration.privateKey -> "private-key",
                RecaptchaConfiguration.publicKey -> "public-key",
                RecaptchaConfiguration.apiVersion -> String.valueOf(version))

        if (theme.isDefined) {
            config += RecaptchaConfiguration.theme -> theme.get
        }

        if (captchaType.isDefined) {
            config += RecaptchaConfiguration.captchaType -> captchaType.get
        }

        new FakeApplication(additionalConfiguration = config)
    }
}
