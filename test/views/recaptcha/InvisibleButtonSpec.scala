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
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{FakeRequest, PlaySpecification, WithApplication}

/**
  * Tests the <code>invisibleButton</code> view template.
  *
  * @author chrisnappin
  */
@RunWith(classOf[JUnitRunner])
class InvisibleButtonSpec extends PlaySpecification {

  "invisibleButton" should {

    // browser prefers french then english
    val request = FakeRequest().withHeaders(("Accept-Language", "fr; q=1.0, en; q=0.5"))

    "render widget with no explicit language" in new WithApplication(getApplication()) with WithWidgetHelper {
      val messages = app.injector.instanceOf[MessagesApi].preferred(request)

      val html = contentAsString(views.html.recaptcha.invisibleButton("myForm", "Submit")(widgetHelper, messages))

      // includes script
      html must contain("<script")
      html must contain("function onRecaptchaSubmit(token)")
      html must contain("document.getElementById(\"myForm\").submit();")

      // button html
      html must contain("<button")
      html must contain("class=\"g-recaptcha\"")
      html must contain("data-sitekey=\"public-key\"")
      html must contain("data-callback=\"onRecaptchaSubmit\"")
      html must contain(">Submit<")
    }

    // force mode

    // language mode
  }

  /**
    * Get the fake application context.
    *
    * @return The application
    */
  private def getApplication(): Application = {
    var config = Map(
      RecaptchaSettings.PrivateKeyConfigProp -> "private-key",
      RecaptchaSettings.PublicKeyConfigProp -> "public-key")

    new GuiceApplicationBuilder().configure(config).build()
  }

  trait WithWidgetHelper extends Scope {
    def app: play.api.Application

    lazy val settings = new RecaptchaSettings(app.configuration)
    lazy val widgetHelper = new WidgetHelper(settings)
  }

}
