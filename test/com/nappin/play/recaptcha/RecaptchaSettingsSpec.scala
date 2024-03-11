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

import org.specs2.mutable.Specification
import play.api.{PlayException, Configuration}
import com.typesafe.config.ConfigException
import RecaptchaSettings._
import scala.concurrent.duration._

/** Tests the <code>RecaptchaSettings</code> class.
  *
  * @author
  *   chrisnappin
  */
class RecaptchaSettingsSpec extends Specification {

  val privateKeyValue = "myprivkey"
  val publicKeyValue = "mypubkey"

  val requestTimeoutValueStr = "100 seconds"
  val requestTimeoutValue = 100.seconds

  // examples of each of the possible configuration settings
  val captchaThemeValue = "dark"
  val captchaTypeValue = "audio"
  val captchaSizeValue = "compact"
  val languageModeValue = "force"
  val forceLanguageValue = "ru"
  val contentSecurityPolicyValue = "default-src 'self';"
  val nonceLengthValue = 30
  val nonceSeedValue = 123456789012345678L

  val mandatoryV2Config =
    Map(PrivateKeyConfigProp -> privateKeyValue, PublicKeyConfigProp -> publicKeyValue)

  "Construction of RecaptchaSettings" should {
    "succeed if only the mandatory configuration was present" in {
      val conf = Configuration.from(mandatoryV2Config)
      val s = new RecaptchaSettings(conf)

      // check mandatory settings
      s.privateKey ==== privateKeyValue
      s.publicKey ==== publicKeyValue

      // check default value were set for the remaining configuration
      s.requestTimeoutMs ==== RequestTimeoutMsDefault.toMillis
      s.theme ==== None
      s.captchaType ==== CaptchaTypeDefault
      s.captchaSize ==== CaptchaSizeDefault
      s.languageMode ==== LanguageModeDefault
      s.forceLanguage ==== None
      s.contentSecurityPolicy ==== ContentSecurityPolicyDefault
      s.nonceLength ==== NonceLengthDefault
      s.nonceSeed ==== None
    }

    "succeed if all possible configuration was present" in {
      val conf = Configuration.from(
        mandatoryV2Config ++ Map(
          RequestTimeoutConfigProp -> requestTimeoutValueStr,
          ThemeConfigProp -> captchaThemeValue,
          CaptchaTypeConfigProp -> captchaTypeValue,
          CaptchaSizeConfigProp -> captchaSizeValue,
          LanguageModeConfigProp -> languageModeValue,
          ForceLanguageConfigProp -> forceLanguageValue,
          ContentSecurityPolicyProp -> contentSecurityPolicyValue,
          NonceLengthProp -> nonceLengthValue,
          NonceSeedProp -> nonceSeedValue
        )
      )
      val s = new RecaptchaSettings(conf)

      // check all settings
      s.privateKey ==== privateKeyValue
      s.publicKey ==== publicKeyValue
      s.requestTimeoutMs ==== requestTimeoutValue.toMillis
      s.theme ==== Some(captchaThemeValue)
      s.captchaType ==== captchaTypeValue
      s.captchaSize === captchaSizeValue
      s.languageMode === languageModeValue
      s.forceLanguage === Some(forceLanguageValue)
      s.contentSecurityPolicy ==== contentSecurityPolicyValue
      s.nonceLength === nonceLengthValue
      s.nonceSeed ==== Some(nonceSeedValue)
    }

    "fail if no configuration" in {
      val conf = Configuration.from(Map())

      new RecaptchaSettings(conf) must throwAn[ConfigException]
    }

    "fail if private key is missing" in {
      val conf = Configuration.from(mandatoryV2Config - PrivateKeyConfigProp)

      new RecaptchaSettings(conf) must throwAn[ConfigException]
    }

    "fail if public key is missing" in {
      val conf = Configuration.from(mandatoryV2Config - PublicKeyConfigProp)

      new RecaptchaSettings(conf) must throwAn[ConfigException]
    }

    "fail if requestTimeout config value can not parsed as a valid duration" in {
      val conf =
        Configuration.from(mandatoryV2Config + (RequestTimeoutConfigProp -> "10 million dollars"))

      new RecaptchaSettings(conf) must throwAn[ConfigException]
    }

    "fail if captcha type is not one of allowed values" in {
      val conf = Configuration.from(mandatoryV2Config + (CaptchaTypeConfigProp -> "movie"))

      new RecaptchaSettings(conf) must throwAn[PlayException]
    }

    "fail if captcha size is not one of allowed values" in {
      val conf = Configuration.from(mandatoryV2Config + (CaptchaSizeConfigProp -> "wibble"))

      new RecaptchaSettings(conf) must throwAn[PlayException]
    }

    "fail if languageMode is force but forceLanguage not set" in {
      val conf = Configuration.from(mandatoryV2Config + (LanguageModeConfigProp -> "force"))

      new RecaptchaSettings(conf) must throwAn[ConfigException]
    }
  }

  "Recaptcha Settings widgetScriptUrl" should {
    "return a secure api v2 url" in {
      val conf = Configuration.from(mandatoryV2Config)

      new RecaptchaSettings(conf).widgetScriptUrl ==== "https://www.google.com/recaptcha/api.js"
    }
  }

  "Recaptcha Settings widgetNoScriptUrl" should {
    "return a secure api v2 url" in {
      val conf = Configuration.from(mandatoryV2Config)

      new RecaptchaSettings(
        conf
      ).widgetNoScriptUrl ==== "https://www.google.com/recaptcha/api/fallback"
    }
  }
}
