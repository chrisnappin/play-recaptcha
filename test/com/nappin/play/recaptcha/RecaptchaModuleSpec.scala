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

import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith
import play.api.Application

import play.api.test.{FakeApplication, WithApplication}

/**
 * Tests the <code>recaptchaModule</code> class.
 *
 * @author Chris Nappi0n
 */
@RunWith(classOf[JUnitRunner])
class RecaptchaModuleSpec extends Specification {

  def recaptchaModule(implicit application: Application) = application.injector.instanceOf[RecaptchaModule]

  "recaptchaModule (api v1)" should {

    "not be enabled if mandatory configuration missing" in
      new WithApplication(new FakeApplication()) {

        recaptchaModule.checkConfiguration must throwA[RecaptchaConfigurationException]
      }

    "be enabled if mandatory configuration present" in
      new WithApplication(new FakeApplication(
        additionalConfiguration = Map(
          RecaptchaConfiguration.privateKey -> "private-key",
          RecaptchaConfiguration.publicKey -> "public-key",
          RecaptchaConfiguration.apiVersion -> "1"))) {
        recaptchaModule.checkConfiguration
      }

    "be enabled if booleans are true or false" in
      new WithApplication(new FakeApplication(
        additionalConfiguration = Map(
          RecaptchaConfiguration.privateKey -> "private-key",
          RecaptchaConfiguration.publicKey -> "public-key",
          RecaptchaConfiguration.apiVersion -> "1",
          RecaptchaConfiguration.useSecureVerifyUrl -> "true",
          RecaptchaConfiguration.useSecureWidgetUrl -> "false"))) {
        recaptchaModule.checkConfiguration
      }

    "be enabled if booleans are yes or no" in
      new WithApplication(new FakeApplication(
        additionalConfiguration = Map(
          RecaptchaConfiguration.privateKey -> "private-key",
          RecaptchaConfiguration.publicKey -> "public-key",
          RecaptchaConfiguration.apiVersion -> "1",
          RecaptchaConfiguration.useSecureVerifyUrl -> "yes",
          RecaptchaConfiguration.useSecureWidgetUrl -> "no"))) {
        recaptchaModule.checkConfiguration
      }

    "not be enabled if useSecureVerifyUrl invalid" in
      new WithApplication(new FakeApplication(
        additionalConfiguration = Map(
          RecaptchaConfiguration.privateKey -> "private-key",
          RecaptchaConfiguration.publicKey -> "public-key",
          RecaptchaConfiguration.apiVersion -> "1",
          RecaptchaConfiguration.useSecureVerifyUrl -> "wibble"))) {
        recaptchaModule.checkConfiguration must throwA[RecaptchaConfigurationException]
      }

    "not be enabled if useSecureWidgetUrl invalid" in
      new WithApplication(new FakeApplication(
        additionalConfiguration = Map(
          RecaptchaConfiguration.privateKey -> "private-key",
          RecaptchaConfiguration.publicKey -> "public-key",
          RecaptchaConfiguration.apiVersion -> "1",
          RecaptchaConfiguration.useSecureWidgetUrl -> "wibble"))) {
        recaptchaModule.checkConfiguration must throwA[RecaptchaConfigurationException]
      }

    "be enabled if language unsupported" in
      new WithApplication(new FakeApplication(
        additionalConfiguration = Map(
          RecaptchaConfiguration.privateKey -> "private-key",
          RecaptchaConfiguration.publicKey -> "public-key",
          RecaptchaConfiguration.apiVersion -> "1",
          RecaptchaConfiguration.defaultLanguage -> "zz"))) {
        recaptchaModule.checkConfiguration
      }

    "api version 1 enabled if valid" in
      new WithApplication(new FakeApplication(
        additionalConfiguration = Map(
          RecaptchaConfiguration.privateKey -> "private-key",
          RecaptchaConfiguration.publicKey -> "public-key",
          RecaptchaConfiguration.apiVersion -> "1"))) {
        recaptchaModule.isApiVersion1 must equalTo(true)
      }
  }

  "recaptchaModule (api v2)" should {

    "not be enabled if mandatory configuration missing" in
      new WithApplication(new FakeApplication()) {
        recaptchaModule.checkConfiguration must throwA[RecaptchaConfigurationException]
      }

    "be enabled if mandatory configuration present" in
      new WithApplication(new FakeApplication(
        additionalConfiguration = Map(
          RecaptchaConfiguration.privateKey -> "private-key",
          RecaptchaConfiguration.publicKey -> "public-key",
          RecaptchaConfiguration.apiVersion -> "2"))) {
        recaptchaModule.checkConfiguration
      }

    "api version 2 enabled if valid" in
      new WithApplication(new FakeApplication(
        additionalConfiguration = Map(
          RecaptchaConfiguration.privateKey -> "private-key",
          RecaptchaConfiguration.publicKey -> "public-key",
          RecaptchaConfiguration.apiVersion -> "2"))) {
        recaptchaModule.isApiVersion1 must equalTo(false)
      }
  }

  "recaptchaModule (invalid api version)" should {

    "not be enabled if api version invalid" in
      new WithApplication(new FakeApplication(
        additionalConfiguration = Map(
          RecaptchaConfiguration.privateKey -> "private-key",
          RecaptchaConfiguration.publicKey -> "public-key",
          RecaptchaConfiguration.apiVersion -> "wibble"))) {
        recaptchaModule.checkConfiguration must throwA[RecaptchaConfigurationException]
      }

    "not be enabled if api version unsupported" in
      new WithApplication(new FakeApplication(
        additionalConfiguration = Map(
          RecaptchaConfiguration.privateKey -> "private-key",
          RecaptchaConfiguration.publicKey -> "public-key",
          RecaptchaConfiguration.apiVersion -> "3"))) {
        recaptchaModule.checkConfiguration must throwA[RecaptchaConfigurationException]
      }

    "api version 1 not enabled if api version unsupported" in
      new WithApplication(new FakeApplication(
        additionalConfiguration = Map(
          RecaptchaConfiguration.privateKey -> "private-key",
          RecaptchaConfiguration.publicKey -> "public-key",
          RecaptchaConfiguration.apiVersion -> "3"))) {
        recaptchaModule.isApiVersion1 must equalTo(false)
      }
  }
}
