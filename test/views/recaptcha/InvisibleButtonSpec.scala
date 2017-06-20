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

import com.nappin.play.recaptcha.{RecaptchaSettings, WidgetHelper}
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.specification.Scope
import play.api.Application
import play.api.i18n.{Lang, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{PlaySpecification, WithApplication}

/**
  * Tests the <code>invisibleButton</code> view template.
  *
  * @author chrisnappin
  */
@RunWith(classOf[JUnitRunner])
class InvisibleButtonSpec extends PlaySpecification {

  "invisibleButton" should {

    "render widget with no explicit language" in new WithApplication(
        getApplication()) with WithWidgetHelper {

      val messages = app.injector.instanceOf[MessagesApi].preferred(Seq(Lang("fr")))

      val html = contentAsString(views.html.recaptcha.invisibleButton("myForm", "Submit")(widgetHelper, messages))

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
    }

    "render widget with language forced" in new WithApplication(
        getApplication(forceLanguage = Some("ru"))) with WithWidgetHelper {

      val messages = app.injector.instanceOf[MessagesApi].preferred(Seq(Lang("fr")))

      val html = contentAsString(views.html.recaptcha.invisibleButton("myForm", "Submit")(widgetHelper, messages))

      // includes script
      html must contain("recaptcha/api.js?hl=ru")
    }

    "render widget with play language" in new WithApplication(
      getApplication(playLanguage = true)) with WithWidgetHelper {

      val messages = app.injector.instanceOf[MessagesApi].preferred(Seq(Lang("fr")))

      val html = contentAsString(views.html.recaptcha.invisibleButton("myForm", "Submit")(widgetHelper, messages))

      // includes script
      html must contain("recaptcha/api.js?hl=fr")
    }

    "render widget with play language and country" in new WithApplication(
      getApplication(playLanguage = true)) with WithWidgetHelper {

      val messages = app.injector.instanceOf[MessagesApi].preferred(Seq(Lang("en", "GB")))

      val html = contentAsString(views.html.recaptcha.invisibleButton("myForm", "Submit")(widgetHelper, messages))

      // includes script
      html must contain("recaptcha/api.js?hl=en-GB")
    }

    "render widget with additional class" in new WithApplication(
      getApplication()) with WithWidgetHelper {

      val messages = app.injector.instanceOf[MessagesApi].preferred(Seq(Lang("fr")))

      val html = contentAsString(views.html.recaptcha.invisibleButton("myForm", "Submit", 'class -> "extraClass")(widgetHelper, messages))

      // button html
      html must contain("class=\"g-recaptcha extraClass\"")
    }

    "render widget with additional attributes" in new WithApplication(
      getApplication()) with WithWidgetHelper {

      val messages = app.injector.instanceOf[MessagesApi].preferred(Seq(Lang("fr")))

      val html = contentAsString(views.html.recaptcha.invisibleButton(
        "myForm", "Submit", 'class -> "extraClass", 'id -> "myId", 'tabindex -> "5")(widgetHelper, messages))

      // button html
      html must contain("class=\"g-recaptcha extraClass\"")
      html must contain("id=\"myId\" tabindex=\"5\"")
    }
  }

  /**
    * Get the fake application context.
    *
    * @param forceLanguage      The force language setting, if any
    * @param playLanguage       Whether to use the play language (default is false)
    * @return The application
    */
  private def getApplication(forceLanguage: Option[String] = None, playLanguage: Boolean = false): Application = {
    var config: Map[String, Any] = Map(
      RecaptchaSettings.PrivateKeyConfigProp -> "private-key",
      RecaptchaSettings.PublicKeyConfigProp -> "public-key")

    if (forceLanguage.isDefined) {
      config += RecaptchaSettings.LanguageModeConfigProp -> "force"
      config += RecaptchaSettings.ForceLanguageConfigProp -> forceLanguage.get
    } else if (playLanguage) {
      config += RecaptchaSettings.LanguageModeConfigProp -> "play"
      config += "play.i18n.langs" -> Seq("fr", "en", "en-US", "en-GB")
    }

    new GuiceApplicationBuilder().configure(config).build()
  }

  trait WithWidgetHelper extends Scope {
    def app: play.api.Application

    lazy val settings = new RecaptchaSettings(app.configuration)
    lazy val widgetHelper = new WidgetHelper(settings)
  }

}
