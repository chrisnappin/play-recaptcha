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
import play.api.Application
import play.api.test.{FakeApplication, PlaySpecification, WithApplication}

/**
 * Tests the <code>recaptchaUrls</code> class.
 *
 * @author Chris Nappin
 */
@RunWith(classOf[JUnitRunner])
class RecaptchaUrlsSpec extends PlaySpecification {

    def recaptchaUrls(implicit application: Application) = application.injector.instanceOf[RecaptchaUrls]


    val noConfig = new FakeApplication(
            additionalConfiguration = Map(
                RecaptchaConfiguration.privateKey -> "private-key",
                RecaptchaConfiguration.publicKey -> "public-key",
                RecaptchaConfiguration.apiVersion -> "1"))

    val insecureConfig = new FakeApplication(
            additionalConfiguration = Map(
                RecaptchaConfiguration.privateKey -> "private-key",
                RecaptchaConfiguration.publicKey -> "public-key",
                RecaptchaConfiguration.apiVersion -> "1",
                RecaptchaConfiguration.useSecureVerifyUrl -> "false",
                RecaptchaConfiguration.useSecureWidgetUrl -> "false"))

    val altInsecureConfig = new FakeApplication(
            additionalConfiguration = Map(
                RecaptchaConfiguration.privateKey -> "private-key",
                RecaptchaConfiguration.publicKey -> "public-key",
                RecaptchaConfiguration.apiVersion -> "1",
                RecaptchaConfiguration.useSecureVerifyUrl -> "no",
                RecaptchaConfiguration.useSecureWidgetUrl -> "no"))

    val secureConfig = new FakeApplication(
            additionalConfiguration = Map(
                RecaptchaConfiguration.privateKey -> "private-key",
                RecaptchaConfiguration.publicKey -> "public-key",
                RecaptchaConfiguration.apiVersion -> "1",
                RecaptchaConfiguration.useSecureVerifyUrl -> "true",
                RecaptchaConfiguration.useSecureWidgetUrl -> "true"))

    val altSecureConfig = new FakeApplication(
            additionalConfiguration = Map(
                RecaptchaConfiguration.privateKey -> "private-key",
                RecaptchaConfiguration.publicKey -> "public-key",
                RecaptchaConfiguration.apiVersion -> "1",
                RecaptchaConfiguration.useSecureVerifyUrl -> "yes",
                RecaptchaConfiguration.useSecureWidgetUrl -> "yes"))

    val v2Config = new FakeApplication(
            additionalConfiguration = Map(
                RecaptchaConfiguration.privateKey -> "private-key",
                RecaptchaConfiguration.publicKey -> "public-key",
                RecaptchaConfiguration.apiVersion -> "2"))

    "getVerifyUrl" should {

        "use insecure url if no configuration present" in new WithApplication(noConfig) {
            recaptchaUrls.getVerifyUrl must equalTo("http://www.google.com/recaptcha/api/verify")
        }

        "use insecure url if explicitly set to false" in new WithApplication(insecureConfig) {
            recaptchaUrls.getVerifyUrl must equalTo("http://www.google.com/recaptcha/api/verify")
        }

        "use insecure url if explicitly set to no" in new WithApplication(altInsecureConfig) {
            recaptchaUrls.getVerifyUrl must equalTo("http://www.google.com/recaptcha/api/verify")
        }

        "use secure url if explicitly set to true" in new WithApplication(secureConfig) {
            recaptchaUrls.getVerifyUrl must equalTo("https://www.google.com/recaptcha/api/verify")
        }

        "use secure url if explicitly set to yes" in new WithApplication(altSecureConfig) {
            recaptchaUrls.getVerifyUrl must equalTo("https://www.google.com/recaptcha/api/verify")
        }

        "use v2 url if api version 2" in new WithApplication(v2Config) {
            recaptchaUrls.getVerifyUrl must equalTo("https://www.google.com/recaptcha/api/siteverify")
        }
    }

    "getWidgetScriptUrl" should {

        "use insecure url if no configuration present" in new WithApplication(noConfig) {
            recaptchaUrls.getWidgetScriptUrl must
                equalTo("http://www.google.com/recaptcha/api/challenge")
        }

        "use insecure url if explicitly set to false" in new WithApplication(insecureConfig) {
            recaptchaUrls.getWidgetScriptUrl must
                equalTo("http://www.google.com/recaptcha/api/challenge")
        }

        "use insecure url if explicitly set to no" in new WithApplication(altInsecureConfig) {
            recaptchaUrls.getWidgetScriptUrl must
                equalTo("http://www.google.com/recaptcha/api/challenge")
        }

        "use secure url if explicitly set to true" in new WithApplication(secureConfig) {
            recaptchaUrls.getWidgetScriptUrl must
                equalTo("https://www.google.com/recaptcha/api/challenge")
        }

        "use secure url if explicitly set to yes" in new WithApplication(altSecureConfig) {
            recaptchaUrls.getWidgetScriptUrl must
                equalTo("https://www.google.com/recaptcha/api/challenge")
        }

        "use v2 url if api version 2" in new WithApplication(v2Config) {
            recaptchaUrls.getWidgetScriptUrl must
                equalTo("https://www.google.com/recaptcha/api.js")
        }
    }

    "getWidgetNoScriptUrl" should {

        "use insecure url if no configuration present" in new WithApplication(noConfig) {
            recaptchaUrls.getWidgetNoScriptUrl must
                equalTo("http://www.google.com/recaptcha/api/noscript")
        }

        "use insecure url if explicitly set to false" in new WithApplication(insecureConfig) {
            recaptchaUrls.getWidgetNoScriptUrl must
                equalTo("http://www.google.com/recaptcha/api/noscript")
        }

        "use insecure url if explicitly set to no" in new WithApplication(altInsecureConfig) {
            recaptchaUrls.getWidgetNoScriptUrl must
                equalTo("http://www.google.com/recaptcha/api/noscript")
        }

        "use secure url if explicitly set to true" in new WithApplication(secureConfig) {
            recaptchaUrls.getWidgetNoScriptUrl must
                equalTo("https://www.google.com/recaptcha/api/noscript")
        }

        "use secure url if explicitly set to yes" in new WithApplication(altSecureConfig) {
            recaptchaUrls.getWidgetNoScriptUrl must
                equalTo("https://www.google.com/recaptcha/api/noscript")
        }

        "use v2 url if api version 2" in new WithApplication(v2Config) {
            recaptchaUrls.getWidgetNoScriptUrl must
                equalTo("https://www.google.com/recaptcha/api/fallback")
        }
    }
}
