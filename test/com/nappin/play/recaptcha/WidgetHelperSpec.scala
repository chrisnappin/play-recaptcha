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

import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith

import play.api.test.{FakeApplication, FakeHeaders, FakeRequest, PlaySpecification, WithApplication}

/**
 * Tests the <code>WidgetHelper</code> object.
 * 
 * @author Chris Nappin
 */
@RunWith(classOf[JUnitRunner])
class WidgetHelperSpec extends PlaySpecification {

    val application = new FakeApplication(additionalConfiguration = Map(
                RecaptchaConfiguration.privateKey -> "private-key",
                RecaptchaConfiguration.publicKey -> "public-key"))
    
    "getPreferredLanguage" should {
                
        "return first matching language (en)" in new WithApplication(application) {
            // browser prefers english then french
            val request = FakeRequest().withHeaders(("Accept-Language", "en; q=1.0, fr; q=0.5"))
            
            // should return english
            WidgetHelper.getPreferredLanguage()(request) must equalTo("en")
        }
        
        "return first matching language (fr)" in new WithApplication(application) {
            // browser prefers french then english
            val request = FakeRequest().withHeaders(("Accept-Language", "fr; q=1.0, en; q=0.5"))
            
            // should return french
            WidgetHelper.getPreferredLanguage()(request) must equalTo("fr")
        }
        
        "return next matching language" in new WithApplication(application) {
            // browser prefers welsh (not supported) then french
            val request = FakeRequest().withHeaders(("Accept-Language", "cy; q=1.0, fr; q=0.5"))
            
            // should return french
            WidgetHelper.getPreferredLanguage()(request) must equalTo("fr")
        }
        
        "return default language if none supported - no default set" in new WithApplication(application) {
            // browser prefers welsh (not supported)
            val request = FakeRequest().withHeaders(("Accept-Language", "cy; q=1.0"))
            
            // should return default language
            WidgetHelper.getPreferredLanguage()(request) must equalTo("en")
        }
        
        "return default language if none supported - alt default set" in new WithApplication(
                new FakeApplication(additionalConfiguration = Map(
                		RecaptchaConfiguration.privateKey -> "private-key",
                		RecaptchaConfiguration.publicKey -> "public-key",
                		RecaptchaConfiguration.defaultLanguage -> "sw"))) {
            
            // browser prefers welsh (not supported)
            val request = FakeRequest().withHeaders(("Accept-Language", "cy; q=1.0"))
            
            // should return configured default language
            WidgetHelper.getPreferredLanguage()(request) must equalTo("sw")
        }
        
        "return default language if no accept-language set" in new WithApplication(application) {
            // no accept-language preference at all
            val request = FakeRequest()
            
            // should return default language
            WidgetHelper.getPreferredLanguage()(request) must equalTo("en")
        }
    }
    
    // only test combinations used in tests above
    "isSupportedLanguage" should {
        
        "match en" in {
            WidgetHelper.isSupportedLanguage("en") must beTrue
        }
        
        "match fr" in {
            WidgetHelper.isSupportedLanguage("fr") must beTrue
        }
        
        "not match cy" in {
            WidgetHelper.isSupportedLanguage("cy") must beFalse
        }
        
        "not match sw" in {
            WidgetHelper.isSupportedLanguage("sw") must beFalse
        }
    }
    
    "getRecaptchaOptions" should {
        
        val application = new FakeApplication(additionalConfiguration = Map(
                "application.langs" -> "en,fr",
        		RecaptchaConfiguration.privateKey -> "private-key",
        		RecaptchaConfiguration.publicKey -> "public-key"))
        
        val request = FakeRequest().withHeaders(("Accept-Language", "fr; q=1.0, en; q=0.5"))
        val lang = request.acceptLanguages(0)
        
        "handle language only" in new WithApplication(application) {
            WidgetHelper.getRecaptchaOptions(None)(request, lang) must equalTo(
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
                    new FakeApplication(additionalConfiguration = Map(
                        "application.langs" -> "en,fr",
                		RecaptchaConfiguration.privateKey -> "private-key",
                		RecaptchaConfiguration.publicKey -> "public-key",
                		RecaptchaConfiguration.theme -> "red"))) {

            WidgetHelper.getRecaptchaOptions(None)(request, lang) must equalTo(
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
                    new FakeApplication(additionalConfiguration = Map(
                        "application.langs" -> "en,fr",
                		RecaptchaConfiguration.privateKey -> "private-key",
                		RecaptchaConfiguration.publicKey -> "public-key",
                		RecaptchaConfiguration.theme -> "red"))) {

            WidgetHelper.getRecaptchaOptions(Some(42))(request, lang) must equalTo(
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
        
        "handle language and tabindex" in new WithApplication(application) {
            WidgetHelper.getRecaptchaOptions(Some(42))(request, lang) must equalTo(
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
}