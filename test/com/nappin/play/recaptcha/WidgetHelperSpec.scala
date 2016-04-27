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
package com.nappin.play.recaptcha

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import play.api.i18n.MessagesApi
import play.api.mvc.Request
import play.api.test.{FakeApplication, FakeRequest, PlaySpecification, WithApplication}
import play.api.{Application}

/**
 * Tests the <code>widgetHelper</code> object.
 *
 * @author Chris Nappin
 */
@RunWith(classOf[JUnitRunner])
class WidgetHelperSpec extends PlaySpecification {

  def widgetHelper(implicit application: Application) = application.injector.instanceOf[WidgetHelper]
  def messages(request:Request[_])(implicit application: Application) = application.injector.instanceOf[MessagesApi].preferred(request)

  val v1Application = new FakeApplication(
            additionalConfiguration = Map(
                RecaptchaConfiguration.privateKey -> "private-key",
                RecaptchaConfiguration.publicKey -> "public-key",
                RecaptchaConfiguration.apiVersion -> "1"))

    val v2Application = new FakeApplication(
            additionalConfiguration = Map(
                RecaptchaConfiguration.privateKey -> "private-key",
                RecaptchaConfiguration.publicKey -> "public-key",
                RecaptchaConfiguration.apiVersion -> "2"))

    "getPreferredLanguage" should {

        "return first matching language (en)" in new WithApplication(v1Application) {
            // browser prefers english then french
            val request = FakeRequest().withHeaders(("Accept-Language", "en; q=1.0, fr; q=0.5"))

            // should return english
            widgetHelper.getPreferredLanguage()(request) must equalTo("en")
        }

        "return first matching language (fr)" in new WithApplication(v1Application) {
            // browser prefers french then english
            val request = FakeRequest().withHeaders(("Accept-Language", "fr; q=1.0, en; q=0.5"))

            // should return french
            widgetHelper.getPreferredLanguage()(request) must equalTo("fr")
        }

        "return next matching language" in new WithApplication(v1Application) {
            // browser prefers welsh (not supported) then french
            val request = FakeRequest().withHeaders(("Accept-Language", "cy; q=1.0, fr; q=0.5"))

            // should return french
            widgetHelper.getPreferredLanguage()(request) must equalTo("fr")
        }

        "return default language if none supported - no default set" in
                new WithApplication(v1Application) {
            // browser prefers welsh (not supported)
            val request = FakeRequest().withHeaders(("Accept-Language", "cy; q=1.0"))

            // should return default language
            widgetHelper.getPreferredLanguage()(request) must equalTo("en")
        }

        "return default language if none supported - alt default set" in new WithApplication(
                new FakeApplication(additionalConfiguration = Map(
                		RecaptchaConfiguration.privateKey -> "private-key",
                		RecaptchaConfiguration.publicKey -> "public-key",
                		RecaptchaConfiguration.defaultLanguage -> "sw"))) {

            // browser prefers welsh (not supported)
            val request = FakeRequest().withHeaders(("Accept-Language", "cy; q=1.0"))

            // should return configured default language
            widgetHelper.getPreferredLanguage()(request) must equalTo("sw")
        }

        "return default language if no accept-language set" in new WithApplication(v1Application) {
            // no accept-language preference at all
            val request = FakeRequest()

            // should return default language
            widgetHelper.getPreferredLanguage()(request) must equalTo("en")
        }
    }

//    // only test combinations used in tests above
//    "isSupportedLanguage" should {
//
//        "match en" in {
//            widgetHelper.isSupportedLanguage("en") must beTrue
//        }
//
//        "match fr" in {
//            widgetHelper.isSupportedLanguage("fr") must beTrue
//        }
//
//        "not match cy" in {
//            widgetHelper.isSupportedLanguage("cy") must beFalse
//        }
//
//        "not match sw" in {
//            widgetHelper.isSupportedLanguage("sw") must beFalse
//        }
//    }

    "getRecaptchaOptions" should {

        val v1Application = new FakeApplication(
                additionalConfiguration = Map(
	                "play.i18n.langs" -> Seq("en", "fr"),
	        		RecaptchaConfiguration.privateKey -> "private-key",
	        		RecaptchaConfiguration.publicKey -> "public-key",
	        		RecaptchaConfiguration.apiVersion -> "1"))

        val request = FakeRequest().withHeaders(("Accept-Language", "fr; q=1.0, en; q=0.5"))

        "handle language only" in new WithApplication(v1Application) {

            widgetHelper.getRecaptchaOptions(None)(request, messages(request)) must equalTo(
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

        "handle language and theme" in new WithApplication(
                new FakeApplication(
                    additionalConfiguration = Map(
                        "play.i18n.langs" -> Seq("en", "fr"),
                		RecaptchaConfiguration.privateKey -> "private-key",
                		RecaptchaConfiguration.publicKey -> "public-key",
                		RecaptchaConfiguration.theme -> "red",
                		RecaptchaConfiguration.apiVersion -> "1"))) {

            widgetHelper.getRecaptchaOptions(None)(request, messages(request)) must equalTo(
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

        "handle language, theme and tabindex" in new WithApplication(
                new FakeApplication(
                    additionalConfiguration = Map(
                        "play.i18n.langs" -> Seq("en", "fr"),
                		RecaptchaConfiguration.privateKey -> "private-key",
                		RecaptchaConfiguration.publicKey -> "public-key",
                		RecaptchaConfiguration.theme -> "red",
                		RecaptchaConfiguration.apiVersion -> "1"))) {

            widgetHelper.getRecaptchaOptions(Some(42))(request, messages(request)) must equalTo(
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

        "handle language and tabindex" in new WithApplication(v1Application) {

            widgetHelper.getRecaptchaOptions(Some(42))(request, messages(request)) must equalTo(
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

        "(v1) include public key" in new WithApplication(v1Application) {
            widgetHelper.getWidgetScriptUrl(None) must endWith("challenge?k=public-key")
        }

        "(v1) include error code if specified" in new WithApplication(v1Application) {
            widgetHelper.getWidgetScriptUrl(Some("error-code")) must
                endWith("challenge?k=public-key&error=error-code")
        }

        "(v2) exclude public key" in new WithApplication(v2Application) {
            widgetHelper.getWidgetScriptUrl(None) must endWith("api.js")
        }

        "(v2) exclude error code if specified" in new WithApplication(v2Application) {
            widgetHelper.getWidgetScriptUrl(Some("error-code")) must endWith("api.js")
        }
    }

    "getWidgetNoScriptUrl" should {

        "(v1) include public key" in new WithApplication(v1Application) {
            widgetHelper.getWidgetNoScriptUrl(None) must endWith("noscript?k=public-key")
        }

        "(v1) include error code if specified" in new WithApplication(v1Application) {
            widgetHelper.getWidgetNoScriptUrl(Some("error-code")) must
                endWith("noscript?k=public-key&error=error-code")
        }

        "(v2) include public key" in new WithApplication(v2Application) {
            widgetHelper.getWidgetNoScriptUrl(None) must endWith("fallback?k=public-key")
        }

        "(v2) exclude error code if specified" in new WithApplication(v2Application) {
            widgetHelper.getWidgetNoScriptUrl(Some("error-code")) must
                endWith("fallback?k=public-key")
        }
    }

    "isApiVersion1" should {

        "identify version 1" in new WithApplication(v1Application) {
            widgetHelper.isApiVersion1 must equalTo(true)
        }

        "identify version 2" in new WithApplication(v2Application) {
            widgetHelper.isApiVersion1 must equalTo(false)
        }
    }

    "getPublicKey" should {

        "(v1) return the public key" in new WithApplication(v1Application) {
            widgetHelper.getPublicKey must equalTo("public-key")
        }

        "(v2) return the public key" in new WithApplication(v2Application) {
            widgetHelper.getPublicKey must equalTo("public-key")
        }
    }
}
