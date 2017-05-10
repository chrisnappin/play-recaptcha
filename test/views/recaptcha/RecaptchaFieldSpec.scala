/*
 * Copyright 2017 Chris Nappin
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

import com.nappin.play.recaptcha.{RecaptchaErrorCode, RecaptchaSettings, RecaptchaVerifier, WidgetHelper}
import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith
import org.specs2.specification.Scope
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.test.{FakeRequest, PlaySpecification, WithApplication}
import play.api.inject.guice.GuiceApplicationBuilder
import java.io.File

/**
 * Tests the <code>recaptchaField</code> view template.
 *
 * @author chrisnappin
 */
@RunWith(classOf[JUnitRunner])
class RecaptchaFieldSpec extends PlaySpecification {

    val scriptApi = "http://www.google.com/recaptcha/api/challenge"
    val noScriptApi = "http://www.google.com/recaptcha/api/noscript"

    // used to bind with
    case class Model(field1: String, field2: Option[Int])

    val modelForm = Form(mapping(
            "field1" -> nonEmptyText,
            "field2" -> optional(number)
        )(Model.apply)(Model.unapply))

    // browser prefers french then english
    val request = FakeRequest().withHeaders(("Accept-Language", "fr; q=1.0, en; q=0.5"))

    "recaptchaField" should {

        val validV2Application = GuiceApplicationBuilder().in(new File("test-conf/"))
	            .configure(Map(
	                "play.i18n.langs" -> Seq("en", "fr"),
                  RecaptchaSettings.PrivateKeyConfigProp -> "private-key",
                  RecaptchaSettings.PublicKeyConfigProp -> "public-key")).build()

        "default to including noscript" in new WithApplication(validV2Application) with WithWidgetHelper {
            val messages = app.injector.instanceOf[MessagesApi].preferred(request)

            val html = contentAsString(views.html.recaptcha.recaptchaField(
                    form = modelForm, fieldName = "myCaptcha")(widgetHelper, request, messages))

            // include v2 recaptcha widget
            html must contain("api.js")
            html must contain("g-recaptcha")

            // no error shown to end user
            html must not contain("<dd class=\"error\">")

            // must include noscript block
            html must contain("<noscript")
            html must contain("g-recaptcha-response")

            // dl doesn't have error class
            html must not contain("<dl class=\"error\"")

            // no tabindex
            html must not contain("data-tabindex")
        }

        "pass includeNoScript to recaptcha widget" in new WithApplication(validV2Application) with WithWidgetHelper {
            val messages = app.injector.instanceOf[MessagesApi].preferred(request)

            val html = contentAsString(views.html.recaptcha.recaptchaField(
                    form = modelForm, fieldName = "myCaptcha", includeNoScript = false)(
                            widgetHelper, request, messages))

            // include v2 recaptcha widget
            html must contain("api.js")
            html must contain("g-recaptcha")

            // dl doesn't have error class
            html must not contain("<dl class=\"error\"")

            // no error shown to end user
            html must not contain("<dd class=\"error\">")

            // must not include noscript block
            html must not contain("<noscript")
            html must not contain("g-recaptcha-response")
        }

        "pass tabindex to recaptcha widget" in new WithApplication(validV2Application) with WithWidgetHelper {
            val messages = app.injector.instanceOf[MessagesApi].preferred(request)

            val html = contentAsString(views.html.recaptcha.recaptchaField(
                    form = modelForm, fieldName = "myCaptcha", tabindex = Some(42))(
                            widgetHelper, request, messages))

            // include v2 recaptcha widget
            html must contain("api.js")
            html must contain("g-recaptcha")

            // dl doesn't have error class
            html must not contain("<dl class=\"error\"")

            // no error shown to end user
            html must not contain("<dd class=\"error\">")

            // explicit tabindex
            html must contain("data-tabindex=\"42\"")
        }

        "show required marker if set" in new WithApplication(validV2Application) with WithWidgetHelper {
            val messages = app.injector.instanceOf[MessagesApi].preferred(request)

            val html = contentAsString(views.html.recaptcha.recaptchaField(
                form = modelForm, fieldName = "myCaptcha", isRequired = true)(
                widgetHelper, request, messages))

            // include v2 recaptcha widget
            html must contain("api.js")
            html must contain("g-recaptcha")

            // dl doesn't have error class
            html must not contain("<dl class=\"error\"")

            // required marker shown to end user
            html must contain("<dd class=\"info\">")
        }

        "default to show required marker" in new WithApplication(validV2Application) with WithWidgetHelper {
            val messages = app.injector.instanceOf[MessagesApi].preferred(request)

            val html = contentAsString(views.html.recaptcha.recaptchaField(
                form = modelForm, fieldName = "myCaptcha")(
                widgetHelper, request, messages))

            // include v2 recaptcha widget
            html must contain("api.js")
            html must contain("g-recaptcha")

            // dl doesn't have error class
            html must not contain("<dl class=\"error\"")

            // required marker shown to end user
            html must contain("<dd class=\"info\">")
        }

        "not show required marker if not set" in new WithApplication(validV2Application) with WithWidgetHelper {
            val messages = app.injector.instanceOf[MessagesApi].preferred(request)

            val html = contentAsString(views.html.recaptcha.recaptchaField(
                form = modelForm, fieldName = "myCaptcha", isRequired = false)(
                widgetHelper, request, messages))

            // include v2 recaptcha widget
            html must contain("api.js")
            html must contain("g-recaptcha")

            // dl doesn't have error class
            html must not contain("<dl class=\"error\"")

            // required marker not shown to end user
            html must not contain("<dd class=\"info\">")
        }

        "show captcha incorrect error" in new WithApplication(validV2Application) with WithWidgetHelper {
            val messages = app.injector.instanceOf[MessagesApi].preferred(request)

            val modelFormWithError = modelForm.withError(
                RecaptchaVerifier.formErrorKey, RecaptchaErrorCode.captchaIncorrect)

            val html = contentAsString(views.html.recaptcha.recaptchaField(
                form = modelFormWithError, fieldName = "myCaptcha")(widgetHelper, request, messages))

            // include v2 recaptcha widget
            html must contain("api.js")
            html must contain("g-recaptcha")

            // dl has error class
            html must contain("<dl class=\"error\"")

            // error shown to end user
            html must contain("<dd class=\"error\">")
        }

        "show recaptcha not reachable error" in new WithApplication(validV2Application) with WithWidgetHelper {
            val messages = app.injector.instanceOf[MessagesApi].preferred(request)

            val modelFormWithError = modelForm.withError(
                RecaptchaVerifier.formErrorKey, RecaptchaErrorCode.recaptchaNotReachable)

            val html = contentAsString(views.html.recaptcha.recaptchaField(
                form = modelFormWithError, fieldName = "myCaptcha")(widgetHelper, request, messages))

            // include v2 recaptcha widget
            html must contain("api.js")
            html must contain("g-recaptcha")

            // dl has error class
            html must contain("<dl class=\"error\"")

            // error shown to end user
            html must contain("<dd class=\"error\">")
        }

        "show api error" in new WithApplication(validV2Application) with WithWidgetHelper {
            val messages = app.injector.instanceOf[MessagesApi].preferred(request)

            val modelFormWithError = modelForm.withError(
                RecaptchaVerifier.formErrorKey, RecaptchaErrorCode.apiError)

            val html = contentAsString(views.html.recaptcha.recaptchaField(
                form = modelFormWithError, fieldName = "myCaptcha")(widgetHelper, request, messages))

            // include v2 recaptcha widget
            html must contain("api.js")
            html must contain("g-recaptcha")

            // dl has error class
            html must contain("<dl class=\"error\"")

            // error shown to end user
            html must contain("<dd class=\"error\">")
        }

        "show response missing error" in new WithApplication(validV2Application) with WithWidgetHelper {
            val messages = app.injector.instanceOf[MessagesApi].preferred(request)

            val modelFormWithError = modelForm.withError(
                RecaptchaVerifier.formErrorKey, RecaptchaErrorCode.responseMissing)

            val html = contentAsString(views.html.recaptcha.recaptchaField(
                form = modelFormWithError, fieldName = "myCaptcha")(widgetHelper, request, messages))

            // include v2 recaptcha widget
            html must contain("api.js")
            html must contain("g-recaptcha")

            // dl has error class
            html must contain("<dl class=\"error\"")

            // error shown to end user
            html must contain("<dd class=\"error\">")
        }

        "ignores other errors" in new WithApplication(validV2Application) with WithWidgetHelper {
            val messages = app.injector.instanceOf[MessagesApi].preferred(request)

            val modelFormWithError = modelForm.withError(
                RecaptchaVerifier.formErrorKey, "wibble")

            val html = contentAsString(views.html.recaptcha.recaptchaField(
                form = modelFormWithError, fieldName = "myCaptcha")(widgetHelper, request, messages))

            // include v2 recaptcha widget
            html must contain("api.js")
            html must contain("g-recaptcha")

            // dl doesn't have error class
            html must not contain("<dl class=\"error\"")

            // error not shown to end user
            html must not contain("<dd class=\"error\">")
        }
    }

    trait WithWidgetHelper extends Scope {
        def app: play.api.Application
        lazy val settings = new RecaptchaSettings(app.configuration)
        lazy val widgetHelper = new WidgetHelper(settings)
    }
}
