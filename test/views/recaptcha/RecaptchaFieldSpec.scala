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

import com.nappin.play.recaptcha.{
  RecaptchaErrorCode,
  RecaptchaSettings,
  RecaptchaVerifier,
  WidgetHelper
}
import play.api.Application
import play.api.data.Form
import play.api.data.Forms.*
import play.api.i18n.{Lang, MessagesApi, MessagesImpl, MessagesProvider}
import play.api.test.{FakeRequest, PlaySpecification, WithApplication}
import play.api.inject.guice.GuiceApplicationBuilder
import java.io.File

import play.api.mvc.{AnyContent, Request}
import views.html.recaptcha.{recaptchaField, recaptchaWidget}

/** Tests the <code>recaptchaField</code> view template.
  *
  * @author
  *   chrisnappin
  */
class RecaptchaFieldSpec extends PlaySpecification:

  // used to bind with
  case class Model(field1: String, field2: Option[Int])
  object Model:
    def unapply(m: Model): Option[(String, Option[Int])] = Some(m.field1, m.field2)

  private val modelForm = Form(
    mapping(
      "field1" -> nonEmptyText,
      "field2" -> optional(number)
    )(Model.apply)(Model.unapply)
  )

  "recaptchaField" should:

    val validV2Application = GuiceApplicationBuilder()
      .in(new File("test-conf/"))
      .configure(
        Map(
          "play.i18n.langs" -> Seq("en", "fr"),
          RecaptchaSettings.PrivateKeyConfigProp -> "private-key",
          RecaptchaSettings.PublicKeyConfigProp -> "public-key"
        )
      )
      .build()

    "include noscript and tabindex" in new WithApplication(validV2Application)
      with WithWidgetHelper:
      override def running() =
        val (template, messagesProvider, request) = createTemplate(app, widgetHelper)

        val html = contentAsString(
          template(modelForm, "myCaptcha", 42, includeNoScript = true, isRequired = false)(
            messagesProvider,
            request
          )
        )

        // include v2 recaptcha widget
        html must contain("api.js")
        html must contain("g-recaptcha")

        // no error shown to end user
        html must not contain ("<dd class=\"error\">")

        // must include noscript block
        html must contain("<noscript")
        html must contain("g-recaptcha-response")

        // dl doesn't have error class
        html must not contain ("<dl class=\"error\"")

        // include tabindex
        html must contain("data-tabindex=\"42\"")

    "exclude noscript and required marker when set" in new WithApplication(validV2Application)
      with WithWidgetHelper:
      override def running() =
        val (template, messagesProvider, request) = createTemplate(app, widgetHelper)

        val html = contentAsString(
          template(modelForm, "myCaptcha", 1, includeNoScript = false, isRequired = false)(
            messagesProvider,
            request
          )
        )

        // include v2 recaptcha widget
        html must contain("api.js")
        html must contain("g-recaptcha")

        // dl doesn't have error class
        html must not contain ("<dl class=\"error\"")

        // no error shown to end user
        html must not contain ("<dd class=\"error\">")

        // must not include noscript block
        html must not contain ("<noscript")
        html must not contain ("g-recaptcha-response")

        // required marker not shown to end user
        html must not contain ("<dd class=\"info\">")

    "show required marker when set" in new WithApplication(validV2Application)
      with WithWidgetHelper:
      override def running() =
        val (template, messagesProvider, request) = createTemplate(app, widgetHelper)

        val html = contentAsString(
          template(modelForm, "myCaptcha", 1, includeNoScript = false, isRequired = true)(
            messagesProvider,
            request
          )
        )

        // include v2 recaptcha widget
        html must contain("api.js")
        html must contain("g-recaptcha")

        // dl doesn't have error class
        html must not contain ("<dl class=\"error\"")

        // required marker shown to end user
        html must contain("<dd class=\"info\">")

    "show captcha incorrect error" in new WithApplication(validV2Application)
      with WithWidgetHelper:
      override def running() =
        val (template, messagesProvider, request) = createTemplate(app, widgetHelper)
        val modelFormWithError =
          modelForm.withError(RecaptchaVerifier.formErrorKey, RecaptchaErrorCode.captchaIncorrect)

        val html = contentAsString(
          template(modelFormWithError, "myCaptcha", 1, includeNoScript = false, isRequired = true)(
            messagesProvider,
            request
          )
        )

        // include v2 recaptcha widget
        html must contain("api.js")
        html must contain("g-recaptcha")

        // dl has error class
        html must contain("<dl class=\"error\"")

        // error shown to end user
        html must contain("<dd class=\"error\">")

    "show recaptcha not reachable error" in new WithApplication(validV2Application)
      with WithWidgetHelper:
      override def running() =
        val (template, messagesProvider, request) = createTemplate(app, widgetHelper)
        val modelFormWithError = modelForm.withError(
          RecaptchaVerifier.formErrorKey,
          RecaptchaErrorCode.recaptchaNotReachable
        )

        val html = contentAsString(
          template(modelFormWithError, "myCaptcha", 1, includeNoScript = false, isRequired = true)(
            messagesProvider,
            request
          )
        )

        // include v2 recaptcha widget
        html must contain("api.js")
        html must contain("g-recaptcha")

        // dl has error class
        html must contain("<dl class=\"error\"")

        // error shown to end user
        html must contain("<dd class=\"error\">")

    "show api error" in new WithApplication(validV2Application) with WithWidgetHelper:
      override def running() =
        val (template, messagesProvider, request) = createTemplate(app, widgetHelper)
        val modelFormWithError =
          modelForm.withError(RecaptchaVerifier.formErrorKey, RecaptchaErrorCode.apiError)

        val html = contentAsString(
          template(modelFormWithError, "myCaptcha", 1, includeNoScript = false, isRequired = true)(
            messagesProvider,
            request
          )
        )

        // include v2 recaptcha widget
        html must contain("api.js")
        html must contain("g-recaptcha")

        // dl has error class
        html must contain("<dl class=\"error\"")

        // error shown to end user
        html must contain("<dd class=\"error\">")

    "show response missing error" in new WithApplication(validV2Application) with WithWidgetHelper:
      override def running() =
        val (template, messagesProvider, request) = createTemplate(app, widgetHelper)
        val modelFormWithError =
          modelForm.withError(RecaptchaVerifier.formErrorKey, RecaptchaErrorCode.responseMissing)

        val html = contentAsString(
          template(modelFormWithError, "myCaptcha", 1, includeNoScript = false, isRequired = true)(
            messagesProvider,
            request
          )
        )

        // include v2 recaptcha widget
        html must contain("api.js")
        html must contain("g-recaptcha")

        // dl has error class
        html must contain("<dl class=\"error\"")

        // error shown to end user
        html must contain("<dd class=\"error\">")

    "ignores other errors" in new WithApplication(validV2Application) with WithWidgetHelper:
      override def running() =
        val (template, messagesProvider, request) = createTemplate(app, widgetHelper)
        val modelFormWithError = modelForm.withError(RecaptchaVerifier.formErrorKey, "wibble")

        val html = contentAsString(
          template(modelFormWithError, "myCaptcha", 1, includeNoScript = false, isRequired = true)(
            messagesProvider,
            request
          )
        )

        // include v2 recaptcha widget
        html must contain("api.js")
        html must contain("g-recaptcha")

        // dl doesn't have error class
        html must not contain ("<dl class=\"error\"")

        // error not shown to end user
        html must not contain ("<dd class=\"error\">")

    "include extra classes and attributes" in new WithApplication(validV2Application)
      with WithWidgetHelper:
      override def running() =
        val (template, messagesProvider, request) = createTemplate(app, widgetHelper)

        val html = contentAsString(
          template(
            modelForm,
            "myCaptcha",
            1,
            includeNoScript = true,
            isRequired = false,
            Symbol("class") -> "extraClass",
            Symbol("bbb") -> "ccc"
          )(messagesProvider, request)
        )

        // extra class and attribute
        html must contain("class=\"g-recaptcha extraClass\"")
        html must contain("bbb=\"ccc\"")

  /** Creates a template, with real dependencies populated.
    * @param app
    *   The current app
    * @param widgetHelper
    *   The widget helper
    * @return
    *   The template instance, a messages provider, and a request
    */
  private def createTemplate(
      app: Application,
      widgetHelper: WidgetHelper
  ): (recaptchaField, MessagesProvider, Request[AnyContent]) =
    val messagesApi = app.injector.instanceOf[MessagesApi]
    val messagesProvider = MessagesImpl(Lang("fr"), messagesApi)
    val widgetTemplate = new recaptchaWidget(widgetHelper)
    val fieldTemplate = new recaptchaField(widgetHelper, widgetTemplate)
    val request = FakeRequest()
    (fieldTemplate, messagesProvider, request)

  trait WithWidgetHelper:
    def app: play.api.Application

    lazy val settings = new RecaptchaSettings(app.configuration)
    lazy val widgetHelper = new WidgetHelper(settings)

