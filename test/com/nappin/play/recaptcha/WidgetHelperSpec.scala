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
package com.nappin.play.recaptcha

import RecaptchaSettings._
import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith
import org.specs2.specification.Scope
import play.api.{Mode, Environment, Configuration}
import play.api.i18n.{MessagesApi, I18nComponents}
import play.api.test.{FakeApplication, WithApplication, FakeRequest, PlaySpecification}

/**
 * Tests the <code>WidgetHelper</code> object.
 *
 * @author chrisnappin
 */
@RunWith(classOf[JUnitRunner])
class WidgetHelperSpec extends PlaySpecification {

    val validV1Settings: Map[String, AnyRef] = Map(
        PrivateKeyConfigProp -> "private-key",
        PublicKeyConfigProp -> "public-key",
        ApiVersionConfigProp -> "1",
        RequestTimeoutConfigProp -> "5 seconds")

    val validV2Settings: Map[String, AnyRef] =  Map(
        PrivateKeyConfigProp -> "private-key",
        PublicKeyConfigProp -> "public-key",
        ApiVersionConfigProp -> "2",
        RequestTimeoutConfigProp -> "5 seconds")

    abstract class WithWidgetHelper(configProps: Map[String, AnyRef]) extends WithApplication(
        FakeApplication(additionalConfiguration = configProps)) with Scope {

        val settings = new RecaptchaSettings(app.configuration)
        val widgetHelper = new WidgetHelper(settings)
    }

    "getPreferredLanguage" should {

        "return first matching language (en)" in new WithWidgetHelper(validV1Settings) {
            // browser prefers english then french
            val request = FakeRequest().withHeaders(("Accept-Language", "en; q=1.0, fr; q=0.5"))

            // should return english
            widgetHelper.preferredLanguage()(request) must equalTo("en")
        }

        "return first matching language (fr)" in new WithWidgetHelper(validV1Settings) {
            // browser prefers french then english
            val request = FakeRequest().withHeaders(("Accept-Language", "fr; q=1.0, en; q=0.5"))

            // should return french
            widgetHelper.preferredLanguage()(request) must equalTo("fr")
        }

        "return next matching language" in new WithWidgetHelper(validV1Settings) {
            // browser prefers welsh (not supported) then french
            val request = FakeRequest().withHeaders(("Accept-Language", "cy; q=1.0, fr; q=0.5"))

            // should return french
            widgetHelper.preferredLanguage()(request) must equalTo("fr")
        }

        "return default language if none supported - no default set" in
                new WithWidgetHelper(validV1Settings) {
            // browser prefers welsh (not supported)
            val request = FakeRequest().withHeaders(("Accept-Language", "cy; q=1.0"))

            // should return default language
            widgetHelper.preferredLanguage()(request) must equalTo("en")
        }

        "return default language if no accept-language set" in new WithWidgetHelper(validV1Settings) {
            // no accept-language preference at all
            val request = FakeRequest()

            // should return default language
            widgetHelper.preferredLanguage()(request) must equalTo("en")
        }
    }

    // only test combinations used in tests above
    "isSupportedLanguage" should new WithWidgetHelper(validV1Settings) {

        "match en" in {
            widgetHelper.isSupportedLanguage("en") must beTrue
        }

        "match fr" in {
            widgetHelper.isSupportedLanguage("fr") must beTrue
        }

        "not match cy" in {
            widgetHelper.isSupportedLanguage("cy") must beFalse
        }

        "not match sw" in {
            widgetHelper.isSupportedLanguage("sw") must beFalse
        }
    }

    "getRecaptchaOptions" should {

        val v1SettingsWithFrench = validV1Settings + ("play.i18n.langs" -> Seq("en", "fr"))
        val request = FakeRequest().withHeaders(("Accept-Language", "fr; q=1.0, en; q=0.5"))

        "handle language only" in new WithWidgetHelper(v1SettingsWithFrench) {
            val messages = app.injector.instanceOf[MessagesApi].preferred(request)

            widgetHelper.recaptchaOptions(None)(request, messages) must equalTo(
                    "var RecaptchaOptions = {\n" +
                    "  lang : 'fr',\n" +
                    "  custom_translations : {\n" +
                    "    visual_challenge : 'Fr-Visual-Challenge',\n" +
                    "    play_again : 'Fr-Play \\u00E9 Again',\n" +
                    "    cant_hear_this : 'Fr-Cant-Hear-This',\n" +
                    "    incorrect_try_again : 'Fr-Incorrect-Try-Again',\n" +
                    "    audio_challenge : 'Fr-Audio \\u00E0 Challenge',\n" +
                    "    privacy_and_terms : 'Fr-Privacy-And-Terms',\n" +
                    "    instructions_visual : 'Fr-Instructions-Visual',\n" +
                    "    image_alt_text : 'Fr-Image-Alt-Text',\n" +
                    "    instructions_audio : 'Fr-Instructions-Audio',\n" +
                    "    refresh_btn : 'Fr-Refresh-Button',\n" +
                    "    help_btn : 'Fr-Help-Button'\n" +
                    "  }\n" +
                    "};")
        }

        "handle language and theme" in new WithWidgetHelper(
            v1SettingsWithFrench ++ Map(ThemeConfigProp -> "red")) {
            val messages = app.injector.instanceOf[MessagesApi].preferred(request)

            widgetHelper.recaptchaOptions(None)(request, messages) must equalTo(
                    "var RecaptchaOptions = {\n" +
                    "  lang : 'fr',\n" +
                    "  theme : 'red',\n" +
                    "  custom_translations : {\n" +
                    "    visual_challenge : 'Fr-Visual-Challenge',\n" +
                    "    play_again : 'Fr-Play \\u00E9 Again',\n" +
                    "    cant_hear_this : 'Fr-Cant-Hear-This',\n" +
                    "    incorrect_try_again : 'Fr-Incorrect-Try-Again',\n" +
                    "    audio_challenge : 'Fr-Audio \\u00E0 Challenge',\n" +
                    "    privacy_and_terms : 'Fr-Privacy-And-Terms',\n" +
                    "    instructions_visual : 'Fr-Instructions-Visual',\n" +
                    "    image_alt_text : 'Fr-Image-Alt-Text',\n" +
                    "    instructions_audio : 'Fr-Instructions-Audio',\n" +
                    "    refresh_btn : 'Fr-Refresh-Button',\n" +
                    "    help_btn : 'Fr-Help-Button'\n" +
                    "  }\n" +
                    "};")
        }

        "handle language, theme and tabindex" in new WithWidgetHelper(
            v1SettingsWithFrench ++ Map(ThemeConfigProp -> "red")) {
            val messages = app.injector.instanceOf[MessagesApi].preferred(request)

            widgetHelper.recaptchaOptions(Some(42))(request, messages) must equalTo(
                    "var RecaptchaOptions = {\n" +
                    "  lang : 'fr',\n" +
                    "  theme : 'red',\n" +
                    "  tabindex : 42,\n" +
                    "  custom_translations : {\n" +
                    "    visual_challenge : 'Fr-Visual-Challenge',\n" +
                    "    play_again : 'Fr-Play \\u00E9 Again',\n" +
                    "    cant_hear_this : 'Fr-Cant-Hear-This',\n" +
                    "    incorrect_try_again : 'Fr-Incorrect-Try-Again',\n" +
                    "    audio_challenge : 'Fr-Audio \\u00E0 Challenge',\n" +
                    "    privacy_and_terms : 'Fr-Privacy-And-Terms',\n" +
                    "    instructions_visual : 'Fr-Instructions-Visual',\n" +
                    "    image_alt_text : 'Fr-Image-Alt-Text',\n" +
                    "    instructions_audio : 'Fr-Instructions-Audio',\n" +
                    "    refresh_btn : 'Fr-Refresh-Button',\n" +
                    "    help_btn : 'Fr-Help-Button'\n" +
                    "  }\n" +
                    "};")
        }

        "handle language and tabindex" in new WithWidgetHelper(v1SettingsWithFrench) {
            val messages = app.injector.instanceOf[MessagesApi].preferred(request)

            widgetHelper.recaptchaOptions(Some(42))(request, messages) must equalTo(
                    "var RecaptchaOptions = {\n" +
                    "  lang : 'fr',\n" +
                    "  tabindex : 42,\n" +
                    "  custom_translations : {\n" +
                    "    visual_challenge : 'Fr-Visual-Challenge',\n" +
                    "    play_again : 'Fr-Play \\u00E9 Again',\n" +
                    "    cant_hear_this : 'Fr-Cant-Hear-This',\n" +
                    "    incorrect_try_again : 'Fr-Incorrect-Try-Again',\n" +
                    "    audio_challenge : 'Fr-Audio \\u00E0 Challenge',\n" +
                    "    privacy_and_terms : 'Fr-Privacy-And-Terms',\n" +
                    "    instructions_visual : 'Fr-Instructions-Visual',\n" +
                    "    image_alt_text : 'Fr-Image-Alt-Text',\n" +
                    "    instructions_audio : 'Fr-Instructions-Audio',\n" +
                    "    refresh_btn : 'Fr-Refresh-Button',\n" +
                    "    help_btn : 'Fr-Help-Button'\n" +
                    "  }\n" +
                    "};")
        }
    }

    "getWidgetScriptUrl" should {

        "(v1) include public key" in new WithWidgetHelper(validV1Settings) {
            widgetHelper.widgetScriptUrl(None) must endWith("challenge?k=public-key")
        }

        "(v1) include error code if specified" in new WithWidgetHelper(validV1Settings) {
            widgetHelper.widgetScriptUrl(Some("error-code")) must
                endWith("challenge?k=public-key&error=error-code")
        }

        "(v2) exclude public key" in new WithWidgetHelper(validV2Settings) {
            widgetHelper.widgetScriptUrl(None) must endWith("api.js")
        }

        "(v2) exclude error code if specified" in new WithWidgetHelper(validV2Settings) {
            widgetHelper.widgetScriptUrl(Some("error-code")) must endWith("api.js")
        }

        "(v2) exclude language if mode is auto" in new WithWidgetHelper(
                validV2Settings ++ Map(LanguageModeConfigProp -> "auto")) {
            widgetHelper.widgetScriptUrl(None) must endWith("api.js")
        }

        "(v2) include language if mode is force" in new WithWidgetHelper(
                validV2Settings ++ Map(LanguageModeConfigProp -> "force",
                        ForceLanguageConfigProp -> "fr")) {
            widgetHelper.widgetScriptUrl(None) must endWith("api.js?hl=fr")
        }

        // TODO: test "play" mode
    }

    "getWidgetNoScriptUrl" should {

        "(v1) include public key" in new WithWidgetHelper(validV1Settings) {
            widgetHelper.widgetNoScriptUrl(None) must endWith("noscript?k=public-key")
        }

        "(v1) include error code if specified" in new WithWidgetHelper(validV1Settings) {
            widgetHelper.widgetNoScriptUrl(Some("error-code")) must
                endWith("noscript?k=public-key&error=error-code")
        }

        "(v2) include public key" in new WithWidgetHelper(validV2Settings) {
            widgetHelper.widgetNoScriptUrl(None) must endWith("fallback?k=public-key")
        }

        "(v2) exclude error code if specified" in new WithWidgetHelper(validV2Settings) {
            widgetHelper.widgetNoScriptUrl(Some("error-code")) must
                endWith("fallback?k=public-key")
        }
    }

    "isApiVersion1" should {

        "identify version 1" in new WithWidgetHelper(validV1Settings) {
            widgetHelper.isApiVersion1 must equalTo(true)
        }

        "identify version 2" in new WithWidgetHelper(validV2Settings) {
            widgetHelper.isApiVersion1 must equalTo(false)
        }
    }

    "getPublicKey" should {

        "(v1) return the public key" in new WithWidgetHelper(validV1Settings) {
            widgetHelper.publicKey must equalTo("public-key")
        }

        "(v2) return the public key" in new WithWidgetHelper(validV2Settings) {
            widgetHelper.publicKey must equalTo("public-key")
        }
    }
}
