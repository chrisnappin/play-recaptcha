/*
 * Copyright 2016 Chris Nappin
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

import com.nappin.play.recaptcha.{WidgetHelper, RecaptchaSettings}

import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith
import org.specs2.specification.Scope

import play.api.i18n.MessagesApi
import play.api.test.{FakeRequest, PlaySpecification, WithApplication}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

/**
  * Tests the <code>recaptchaWidget</code> view template.
  *
  * @author chrisnappin
  */
@RunWith(classOf[JUnitRunner])
class RecaptchaWidgetSpec extends PlaySpecification {

  val scriptApi = "http://www.google.com/recaptcha/api/challenge"

  val noScriptApi = "http://www.google.com/recaptcha/api/noscript"

  // browser prefers french then english
  val request = FakeRequest().withHeaders(("Accept-Language", "fr; q=1.0, en; q=0.5"))

  "recaptchaWidget" should {

    "render widget with noscript block" in new WithApplication(getApplication()) with WithWidgetHelper {
      val messages = app.injector.instanceOf[MessagesApi].preferred(request)

      val html = contentAsString(views.html.recaptcha.recaptchaWidget(true, 1)(widgetHelper, request, messages))

      // includes script
      html must contain("<script")
      html must contain("g-recaptcha")
      html must contain("data-sitekey=\"public-key\"")

      // default theme, type, size
      html must contain("data-theme=\"light\"")
      html must contain("data-type=\"image\"")
      html must contain("data-size=\"normal\"")

      // tabindex
      html must contain ("data-tabindex=\"1\"")

      // includes noscript block
      html must contain("<noscript")
      html must contain("g-recaptcha-response")
      html must contain("fallback?k=public-key")
    }

    "render widget without noscript" in new WithApplication(getApplication()) with WithWidgetHelper {
      val messages = app.injector.instanceOf[MessagesApi].preferred(request)

      val html = contentAsString(views.html.recaptcha.recaptchaWidget(false, 1)(widgetHelper, request, messages))

      // includes script
      html must contain("<script")
      html must contain("g-recaptcha")
      html must contain("data-sitekey=\"public-key\"")

      // default theme, type, size
      html must contain("data-theme=\"light\"")
      html must contain("data-type=\"image\"")
      html must contain("data-size=\"normal\"")

      // doesn't include noscript block
      html must not contain ("<noscript")
      html must not contain ("g-recaptcha-response")
      html must not contain ("fallback?k=public-key")
    }

    "render widget with theme" in new WithApplication(getApplication(Some("dark"))) with WithWidgetHelper {
      val messages = app.injector.instanceOf[MessagesApi].preferred(request)

      val html = contentAsString(views.html.recaptcha.recaptchaWidget(true, 1)(widgetHelper, request, messages))

      // explicit theme, default type, size
      html must contain("data-theme=\"dark\"")
      html must contain("data-type=\"image\"")
      html must contain("data-size=\"normal\"")
    }

    "render widget with type" in new WithApplication(getApplication(captchaType = Some("audio")))
        with WithWidgetHelper {
      val messages = app.injector.instanceOf[MessagesApi].preferred(request)

      val html = contentAsString(views.html.recaptcha.recaptchaWidget(true, 1)(widgetHelper, request, messages))

      // default theme, explicit type, default size
      html must contain("data-theme=\"light\"")
      html must contain("data-type=\"audio\"")
      html must contain("data-size=\"normal\"")
    }

    "render v2 widget with size" in new WithApplication(getApplication(captchaSize = Some("compact")))
        with WithWidgetHelper {
      val messages = app.injector.instanceOf[MessagesApi].preferred(request)

      val html = contentAsString(views.html.recaptcha.recaptchaWidget(true, 1)(widgetHelper, request, messages))

      // default theme and type, explicit size
      html must contain("data-theme=\"light\"")
      html must contain("data-type=\"image\"")
      html must contain("data-size=\"compact\"")
    }

    "render widget with extra classes" in new WithApplication(getApplication()) with WithWidgetHelper {
      val messages = app.injector.instanceOf[MessagesApi].preferred(request)

      val html = contentAsString(views.html.recaptcha.recaptchaWidget(true, 1, 'class -> "extra")(
        widgetHelper, request, messages))

      // recaptcha and extra class
      html must contain("class=\"g-recaptcha extra\"")
    }

    "render widget with extra attributes" in new WithApplication(getApplication()) with WithWidgetHelper {
      val messages = app.injector.instanceOf[MessagesApi].preferred(request)

      val html = contentAsString(views.html.recaptcha.recaptchaWidget(true, 1, 'class -> "extra", 'aaa -> "bbb",
        'ccc -> "ddd")(widgetHelper, request, messages))

      // recaptcha and extra class
      html must contain("class=\"g-recaptcha extra\"")

      // extra attributes (not class)
      html must contain("aaa=\"bbb\" ccc=\"ddd\"")
    }
  }

  /**
    * Get the fake application context.
    *
    * @param theme       The configured theme (if any)
    * @param captchaType The captcha type (if any)
    * @param captchaSize The captcha size (if any)
    * @return The application
    */
  private def getApplication(theme: Option[String] = None, captchaType: Option[String] = None,
                             captchaSize: Option[String] = None): Application = {
    var config = Map(
      RecaptchaSettings.PrivateKeyConfigProp -> "private-key",
      RecaptchaSettings.PublicKeyConfigProp -> "public-key")

    if (theme.isDefined) {
      config += RecaptchaSettings.ThemeConfigProp -> theme.get
    }

    if (captchaType.isDefined) {
      config += RecaptchaSettings.CaptchaTypeConfigProp -> captchaType.get
    }

    if (captchaSize.isDefined) {
      config += RecaptchaSettings.CaptchaSizeConfigProp -> captchaSize.get
    }

    new GuiceApplicationBuilder().configure(config).build()
  }

  trait WithWidgetHelper extends Scope {
    def app: play.api.Application

    lazy val settings = new RecaptchaSettings(app.configuration)
    lazy val widgetHelper = new WidgetHelper(settings)
  }

}
