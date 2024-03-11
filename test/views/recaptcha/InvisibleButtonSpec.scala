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
import views.html.recaptcha.invisibleButton

/** Tests the <code>invisibleButton</code> view template.
  *
  * @author
  *   chrisnappin
  */
class InvisibleButtonSpec extends PlaySpecification {

  "invisibleButton" should {

    "render widget with no explicit language" in new WithApplication(getApplication())
      with WithWidgetHelper {
      override def running() = {
        val (template, messagesProvider, request) = createTemplate(app, widgetHelper)
        val html = contentAsString(template("myForm", "Submit")(messagesProvider, request))

        // includes script
        html must contain("<script")
        html must contain("recaptcha/api.js")
        html must contain("function onRecaptchaSubmit(token)")
        html must contain("document.getElementById(\"myForm\").submit();")

        // button html
        html must contain("<button")
        html must contain("class=\"g-recaptcha\"")
        html must contain("data-sitekey=\"public-key\"")
        html must contain("data-callback=\"onRecaptchaSubmit\"")
        html must contain(">Submit<")

        // no request attribute so no nonces
        html must not contain ("nonce=")
      }
    }

    "render widget with language forced" in new WithApplication(
      getApplication(forceLanguage = Some("ru"))
    ) with WithWidgetHelper {
      override def running() = {
        val (template, messagesProvider, request) = createTemplate(app, widgetHelper)
        val html = contentAsString(template("myForm", "Submit")(messagesProvider, request))

        // includes script
        html must contain("recaptcha/api.js?hl=ru")
      }
    }

    "render widget with play language" in new WithApplication(getApplication(playLanguage = true))
      with WithWidgetHelper {
      override def running() = {
        val (template, messagesProvider, request) = createTemplate(app, widgetHelper)
        val html = contentAsString(template("myForm", "Submit")(messagesProvider, request))

        // includes script
        html must contain("recaptcha/api.js?hl=fr")
      }
    }

    "render widget with play language and country" in new WithApplication(
      getApplication(playLanguage = true)
    ) with WithWidgetHelper {
      override def running() = {
        val (template, messagesProvider, request) =
          createTemplate(app, widgetHelper, Lang("en", "GB"))
        val html = contentAsString(template("myForm", "Submit")(messagesProvider, request))

        // includes script
        html must contain("recaptcha/api.js?hl=en-GB")
      }
    }

    "render widget with additional class" in new WithApplication(getApplication())
      with WithWidgetHelper {
      override def running() = {
        val (template, messagesProvider, request) = createTemplate(app, widgetHelper)
        val html = contentAsString(
          template("myForm", "Submit", Symbol("class") -> "extraClass")(messagesProvider, request)
        )

        // button html
        html must contain("class=\"g-recaptcha extraClass\"")
      }
    }

    "render widget with additional attributes" in new WithApplication(getApplication())
      with WithWidgetHelper {
      override def running() = {
        val (template, messagesProvider, request) = createTemplate(app, widgetHelper)
        val html = contentAsString(
          template(
            "myForm",
            "Submit",
            Symbol("class") -> "extraClass",
            Symbol("id") -> "myId",
            Symbol("tabindex") -> "5"
          )(messagesProvider, request)
        )

        // button html
        html must contain("class=\"g-recaptcha extraClass\"")
        html must contain("id=\"myId\" tabindex=\"5\"")
      }
    }

    "render widget with nonces if request attribute set" in new WithApplication(getApplication())
      with WithWidgetHelper {
      override def running() = {
        val (template, messagesProvider, request) = createTemplate(app, widgetHelper)
        val html = contentAsString(
          template("myForm", "Submit")(
            messagesProvider,
            request.addAttr(NonceRequestAttributes.Nonce, "1234abcd")
          )
        )

        // recaptcha javascript block has nonce
        html must contain("<script nonce=\"1234abcd\" type=\"text/javascript\" src=")

        // inline javascript block has nonce
        html must contain("<script nonce=\"1234abcd\">")
      }
    }
  }

  /** Get the fake application context.
    *
    * @param forceLanguage
    *   The force language setting, if any
    * @param playLanguage
    *   Whether to use the play language (default is false)
    * @return
    *   The application
    */
  private def getApplication(
      forceLanguage: Option[String] = None,
      playLanguage: Boolean = false
  ): Application = {
    var config: Map[String, Any] = Map(
      RecaptchaSettings.PrivateKeyConfigProp -> "private-key",
      RecaptchaSettings.PublicKeyConfigProp -> "public-key"
    )

    if forceLanguage.isDefined then {
      config += RecaptchaSettings.LanguageModeConfigProp -> "force"
      config += RecaptchaSettings.ForceLanguageConfigProp -> forceLanguage.get
    } else if playLanguage then {
      config += RecaptchaSettings.LanguageModeConfigProp -> "play"
      config += "play.i18n.langs" -> Seq("fr", "en", "en-US", "en-GB")
    }

    new GuiceApplicationBuilder().configure(config).build()
  }

  /** Creates the template instance.
    * @param app
    *   The current app
    * @param widgetHelper
    *   The widget helper
    * @param lang
    *   The language to use in the messages provider (default is french)
    * @return
    *   The template, a messages provider and a request
    */
  private def createTemplate(
      app: Application,
      widgetHelper: WidgetHelper,
      lang: Lang = Lang("fr")
  ): (invisibleButton, MessagesProvider, Request[AnyContent]) = {
    val messagesApi = app.injector.instanceOf[MessagesApi]
    val messagesProvider = MessagesImpl(lang, messagesApi)
    val template = new invisibleButton(widgetHelper)
    val request = FakeRequest()
    (template, messagesProvider, request)
  }

  trait WithWidgetHelper {
    def app: play.api.Application

    lazy val settings = new RecaptchaSettings(app.configuration)
    lazy val widgetHelper = new WidgetHelper(settings)
  }
}
