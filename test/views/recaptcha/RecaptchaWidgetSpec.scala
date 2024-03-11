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

import com.nappin.play.recaptcha.{NonceRequestAttributes, RecaptchaSettings, WidgetHelper}
import play.api.Application
import play.api.i18n.{Lang, MessagesApi, MessagesImpl, MessagesProvider}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContent, Request}
import play.api.test.{FakeRequest, PlaySpecification, WithApplication}
import views.html.recaptcha.recaptchaWidget

/** Tests the <code>recaptchaWidget</code> view template.
  *
  * @author
  *   chrisnappin
  */
class RecaptchaWidgetSpec extends PlaySpecification {

  "recaptchaWidget" should {

    "render widget with noscript block" in new WithApplication(getApplication())
      with WithWidgetHelper {
      override def running() = {
        val (template, messagesProvider, request) = createTemplate(app, widgetHelper)
        val html = contentAsString(template(true, 1)(messagesProvider, request))

        // includes script
        html must contain("<script")
        html must contain("g-recaptcha")
        html must contain("data-sitekey=\"public-key\"")

        // default theme, type, size
        html must contain("data-theme=\"light\"")
        html must contain("data-type=\"image\"")
        html must contain("data-size=\"normal\"")

        // tabindex
        html must contain("data-tabindex=\"1\"")

        // includes noscript block
        html must contain("<noscript")
        html must contain("g-recaptcha-response")
        html must contain("fallback?k=public-key")

        // no request attribute so no nonce
        html must not contain ("nonce=")
      }
    }

    "render widget without noscript" in new WithApplication(getApplication())
      with WithWidgetHelper {
      override def running() = {
        val (template, messagesProvider, request) = createTemplate(app, widgetHelper)
        val html = contentAsString(template(false, 1)(messagesProvider, request))

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
    }

    "render widget with theme" in new WithApplication(getApplication(Some("dark")))
      with WithWidgetHelper {
      override def running() = {
        val (template, messagesProvider, request) = createTemplate(app, widgetHelper)
        val html = contentAsString(template(true, 1)(messagesProvider, request))

        // explicit theme, default type, size
        html must contain("data-theme=\"dark\"")
        html must contain("data-type=\"image\"")
        html must contain("data-size=\"normal\"")
      }
    }

    "render widget with type" in new WithApplication(getApplication(captchaType = Some("audio")))
      with WithWidgetHelper {
      override def running() = {
        val (template, messagesProvider, request) = createTemplate(app, widgetHelper)
        val html = contentAsString(template(true, 1)(messagesProvider, request))

        // default theme, explicit type, default size
        html must contain("data-theme=\"light\"")
        html must contain("data-type=\"audio\"")
        html must contain("data-size=\"normal\"")
      }
    }

    "render v2 widget with size" in new WithApplication(
      getApplication(captchaSize = Some("compact"))
    ) with WithWidgetHelper {
      override def running() = {
        val (template, messagesProvider, request) = createTemplate(app, widgetHelper)
        val html = contentAsString(template(true, 1)(messagesProvider, request))

        // default theme and type, explicit size
        html must contain("data-theme=\"light\"")
        html must contain("data-type=\"image\"")
        html must contain("data-size=\"compact\"")
      }
    }

    "render widget with extra classes" in new WithApplication(getApplication())
      with WithWidgetHelper {
      override def running() = {
        val (template, messagesProvider, request) = createTemplate(app, widgetHelper)
        val html =
          contentAsString(template(true, 1, Symbol("class") -> "extra")(messagesProvider, request))

        // recaptcha and extra class
        html must contain("class=\"g-recaptcha extra\"")
      }
    }

    "render widget with extra attributes" in new WithApplication(getApplication())
      with WithWidgetHelper {
      override def running() = {
        val (template, messagesProvider, request) = createTemplate(app, widgetHelper)
        val html = contentAsString(
          template(
            true,
            1,
            Symbol("class") -> "extra",
            Symbol("aaa") -> "bbb",
            Symbol("ccc") -> "ddd"
          )(messagesProvider, request)
        )

        // recaptcha and extra class
        html must contain("class=\"g-recaptcha extra\"")

        // extra attributes (not class)
        html must contain("aaa=\"bbb\" ccc=\"ddd\"")
      }
    }

    "render widget with nonce if request attribute set" in new WithApplication(getApplication())
      with WithWidgetHelper {
      override def running() = {
        val (template, messagesProvider, request) = createTemplate(app, widgetHelper)
        val html = contentAsString(
          template(true, 1)(
            messagesProvider,
            request.addAttr(NonceRequestAttributes.Nonce, "1234abcd")
          )
        )

        // recaptcha javascript block has nonce
        html must contain("<script type=\"text/javascript\" nonce=\"1234abcd\" src=")
      }
    }
  }

  /** Get the fake application context.
    *
    * @param theme
    *   The configured theme (if any)
    * @param captchaType
    *   The captcha type (if any)
    * @param captchaSize
    *   The captcha size (if any)
    * @return
    *   The application
    */
  private def getApplication(
      theme: Option[String] = None,
      captchaType: Option[String] = None,
      captchaSize: Option[String] = None
  ): Application = {
    var config = Map(
      RecaptchaSettings.PrivateKeyConfigProp -> "private-key",
      RecaptchaSettings.PublicKeyConfigProp -> "public-key"
    )

    if theme.isDefined then {
      config += RecaptchaSettings.ThemeConfigProp -> theme.get
    }

    if captchaType.isDefined then {
      config += RecaptchaSettings.CaptchaTypeConfigProp -> captchaType.get
    }

    if captchaSize.isDefined then {
      config += RecaptchaSettings.CaptchaSizeConfigProp -> captchaSize.get
    }

    new GuiceApplicationBuilder().configure(config).build()
  }

  /** Creates a new template instance.
    * @param app
    *   The current app
    * @param widgetHelper
    *   The widget helper
    * @return
    *   The template, a messages provider and a request
    */
  private def createTemplate(
      app: Application,
      widgetHelper: WidgetHelper
  ): (recaptchaWidget, MessagesProvider, Request[AnyContent]) = {
    val messagesApi = app.injector.instanceOf[MessagesApi]
    val messagesProvider = MessagesImpl(Lang("fr"), messagesApi)
    val template = new recaptchaWidget(widgetHelper)
    val request = FakeRequest()
    (template, messagesProvider, request)
  }

  trait WithWidgetHelper {
    def app: play.api.Application

    lazy val settings = new RecaptchaSettings(app.configuration)
    lazy val widgetHelper = new WidgetHelper(settings)
  }

}
