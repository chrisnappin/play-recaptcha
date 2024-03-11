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
package com.nappin.play.recaptcha

import RecaptchaSettings._
import play.api.data.{Form, FormError}
import play.api.data.Forms.{mapping, nonEmptyText, number, optional}
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{PlaySpecification, WithApplication}

/** Tests the <code>WidgetHelper</code> object.
  *
  * @author
  *   chrisnappin
  */
class WidgetHelperSpec extends PlaySpecification {

  // used to bind with
  case class Model(field1: String, field2: Option[Int])
  object Model {
    def unapply(m: Model): Option[(String, Option[Int])] = Some(m.field1, m.field2)
  }

  val modelForm = Form(
    mapping(
      "field1" -> nonEmptyText,
      "field2" -> optional(number)
    )(Model.apply)(Model.unapply)
  )

  val validV2Settings: Map[String, String] = Map(
    PrivateKeyConfigProp -> "private-key",
    PublicKeyConfigProp -> "public-key",
    RequestTimeoutConfigProp -> "5 seconds"
  )

  abstract class WithWidgetHelper(
      configProps: Map[String, Any],
      language: Option[Lang] = Some(Lang("en"))
  ) extends WithApplication(GuiceApplicationBuilder().configure(configProps).build()) {
    lazy val settings = new RecaptchaSettings(app.configuration)
    lazy val widgetHelper = new WidgetHelper(settings)
    implicit lazy val messages: Messages =
      app.injector.instanceOf[MessagesApi].preferred(Seq.from(language))
  }

  "getWidgetScriptUrl" should {

    "exclude public key" in new WithWidgetHelper(validV2Settings, Some(Lang("fr"))) {
      override def running() = {
        widgetHelper.widgetScriptUrl(None) must endWith("api.js")
      }
    }

    "exclude error code if specified" in new WithWidgetHelper(validV2Settings, Some(Lang("fr"))) {
      override def running() = {
        widgetHelper.widgetScriptUrl(Some("error-code")) must endWith("api.js")
      }
    }

    "exclude language if mode is auto" in new WithWidgetHelper(
      validV2Settings ++ Map(LanguageModeConfigProp -> "auto"),
      Some(Lang("fr"))
    ) {
      override def running() = {
        widgetHelper.widgetScriptUrl(None) must endWith("api.js")
      }
    }

    "include language if mode is force" in new WithWidgetHelper(
      validV2Settings ++ Map(LanguageModeConfigProp -> "force", ForceLanguageConfigProp -> "fr"),
      Some(Lang("fr"))
    ) {
      override def running() = {
        widgetHelper.widgetScriptUrl(None) must endWith("api.js?hl=fr")
      }
    }

    "include language (only) if mode is play" in new WithWidgetHelper(
      validV2Settings ++ Map(LanguageModeConfigProp -> "play", "play.i18n.langs" -> Seq("fr")),
      None
    ) {
      override def running() = {
        // no browser locale, should just use the default language set above...
        widgetHelper.widgetScriptUrl(None) must endWith("api.js?hl=fr")
      }
    }

    "include language and country if mode is play" in new WithWidgetHelper(
      validV2Settings ++ Map(
        LanguageModeConfigProp -> "play",
        "play.i18n.langs" -> Seq("en", "en-US", "en-GB")
      ),
      Some(Lang("en", "GB"))
    ) {
      override def running() = {
        widgetHelper.widgetScriptUrl(None) must endWith("api.js?hl=en-GB")
      }
    }

    "include just language if mode is play" in new WithWidgetHelper(
      validV2Settings ++ Map(
        LanguageModeConfigProp -> "play",
        "play.i18n.langs" -> Seq("en", "en-US", "en-GB")
      ),
      Some(Lang("en", "US"))
    ) {
      override def running() = {
        // en-US isn't a supported country variant, so should just use the language code
        widgetHelper.widgetScriptUrl(None) must endWith("api.js?hl=en")
      }
    }
  }

  "getWidgetNoScriptUrl" should {

    "include public key" in new WithWidgetHelper(validV2Settings) {
      override def running() = {
        widgetHelper.widgetNoScriptUrl(None) must endWith("fallback?k=public-key")
      }
    }

    "exclude error code if specified" in new WithWidgetHelper(validV2Settings) {
      override def running() = {
        widgetHelper.widgetNoScriptUrl(Some("error-code")) must endWith("fallback?k=public-key")
      }
    }
  }

  "getPublicKey" should {

    "return the public key" in new WithWidgetHelper(validV2Settings) {
      override def running() = {
        widgetHelper.publicKey must equalTo("public-key")
      }
    }
  }

  "getFieldError" should {

    "return None if no error" in new WithWidgetHelper(validV2Settings) {
      override def running() = {
        widgetHelper.getFieldError(modelForm) must beNone
      }
    }

    "return error if captcha incorrect" in new WithWidgetHelper(validV2Settings) {
      override def running() = {
        val modelFormWithError =
          modelForm.withError(RecaptchaVerifier.formErrorKey, RecaptchaErrorCode.captchaIncorrect)

        widgetHelper.getFieldError(modelFormWithError) must beSome("Error-CaptchaIncorrect")
      }
    }

    "return error if recaptcha not reachable" in new WithWidgetHelper(validV2Settings) {
      override def running() = {
        val modelFormWithError = modelForm.withError(
          RecaptchaVerifier.formErrorKey,
          RecaptchaErrorCode.recaptchaNotReachable
        )

        widgetHelper.getFieldError(modelFormWithError) must beSome("Error-RecaptchaNotReachable")
      }
    }

    "return error if api error" in new WithWidgetHelper(validV2Settings) {
      override def running() = {
        val modelFormWithError =
          modelForm.withError(RecaptchaVerifier.formErrorKey, RecaptchaErrorCode.apiError)

        widgetHelper.getFieldError(modelFormWithError) must beSome("Error-ApiError")
      }
    }

    "return error if response missing" in new WithWidgetHelper(validV2Settings) {
      override def running() = {
        val modelFormWithError =
          modelForm.withError(RecaptchaVerifier.formErrorKey, RecaptchaErrorCode.responseMissing)

        widgetHelper.getFieldError(modelFormWithError) must beSome("Error-Required")
      }
    }

    "return None if other error" in new WithWidgetHelper(validV2Settings) {
      override def running() = {
        val modelFormWithError = modelForm.withError(RecaptchaVerifier.formErrorKey, "wibble")

        widgetHelper.getFieldError(modelFormWithError) must beNone
      }
    }
  }

  "resolveRecaptchaErrors" should {

    "return existing errors unchanged" in new WithWidgetHelper(validV2Settings) {
      override def running() = {
        val input = modelForm
          .withError("field1", "error-key1")
          .withError("field2", "error-key2")
        val result = widgetHelper.resolveRecaptchaErrors("captcha", input)

        result.hasErrors must equalTo(true)
        result.errors must equalTo(
          Seq(FormError("field1", "error-key1"), FormError("field2", "error-key2"))
        )
      }
    }

    "resolve recaptcha error (en)" in new WithWidgetHelper(validV2Settings) {
      override def running() = {
        val input = modelForm
          .withError("field1", "error-key1")
          .withError("field2", "error-key2")
          .withError(RecaptchaVerifier.formErrorKey, RecaptchaErrorCode.captchaIncorrect)
        val result = widgetHelper.resolveRecaptchaErrors("captcha", input)

        result.hasErrors must equalTo(true)
        result.errors must equalTo(
          Seq(
            FormError("captcha", "Error-CaptchaIncorrect"), // from test-conf/messages
            FormError("field1", "error-key1"),
            FormError("field2", "error-key2")
          )
        )
      }
    }
  }

  "formatClass" should {

    "handle main class and no args" in new WithWidgetHelper(validV2Settings) {
      override def running() = {
        widgetHelper.formatClass("main") must equalTo("main")
      }
    }

    "ignore non-class args" in new WithWidgetHelper(validV2Settings) {
      override def running() = {
        widgetHelper.formatClass("main", Symbol("other") -> "wibble") must equalTo("main")
      }
    }

    "include single class args" in new WithWidgetHelper(validV2Settings) {
      override def running() = {
        widgetHelper.formatClass(
          "main",
          Symbol("class") -> "extra",
          Symbol("other") -> "wibble"
        ) must equalTo("main extra")
      }
    }

    "include multiple class args" in new WithWidgetHelper(validV2Settings) {
      override def running() = {
        widgetHelper.formatClass(
          "main",
          Symbol("class") -> "extra1",
          Symbol("other") -> "wibble",
          Symbol("class") -> "extra2"
        ) must
          equalTo("main extra1 extra2")
      }
    }
  }

  "formatOtherAttributes" should {

    "produce nothing if no args" in new WithWidgetHelper(validV2Settings) {
      override def running() = {
        widgetHelper.formatOtherAttributes() must equalTo("")
      }
    }

    "ignore class args" in new WithWidgetHelper(validV2Settings) {
      override def running() = {
        widgetHelper.formatOtherAttributes(
          Symbol("class") -> "extra",
          Symbol("other") -> "wibble"
        ) must equalTo("other=\"wibble\"")
      }
    }

    "include multiple args" in new WithWidgetHelper(validV2Settings) {
      override def running() = {
        widgetHelper.formatOtherAttributes(
          Symbol("class") -> "extra",
          Symbol("aaa") -> "bbb",
          Symbol("ccc") -> "ddd"
        ) must
          equalTo("aaa=\"bbb\" ccc=\"ddd\"")
      }
    }
  }
}
