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

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.{PlayException, Configuration}
import com.typesafe.config.ConfigException

/**
 * Tests the <code>RecaptchaSettings</code> class.
 *
 * @author chrisnappin
 */
@RunWith(classOf[JUnitRunner])
class RecaptchaSettingsSpec extends Specification {
    import RecaptchaSettings._

    // Defaults
    import scala.concurrent.duration._

    val privateKeyValue = "myprivkey"
    val publicKeyValue = "mypubkey"
    val apiVersionValue = 2

    val requestTimeoutValueStr = "100 seconds"
    val requestTimeoutValue = 100.seconds

    // V1
    val v1ApiVersionValue = 1
    val v1ThemeValue = "blackglass"
    val defaultLanguageValue = "fr"
    val useSecureWidgetUrlValue = true
    val useSecureVerifyUrlValue = true
    val mandatoryV1Config = Map(
        PrivateKeyConfigProp -> privateKeyValue,
        PublicKeyConfigProp -> publicKeyValue,
        ApiVersionConfigProp -> v1ApiVersionValue)

    // V2
    val v2ThemeValue = "dark"
    val captchaTypeValue = "audio"
    val captchaSizeValue = "compact"
    val languageModeValue = "force"
    val forceLanguageValue = "ru"
    val mandatoryV2Config = Map (
		PrivateKeyConfigProp -> privateKeyValue,
		PublicKeyConfigProp -> publicKeyValue,
		ApiVersionConfigProp -> apiVersionValue)

    "Construction of RecaptchaSettings" should {
    	"succeed if mandatory configuration only (v2) present" in {
    		val conf = Configuration.from(mandatoryV2Config)

			val s = new RecaptchaSettings(conf)
			s.privateKey ==== privateKeyValue
			s.publicKey ==== publicKeyValue
			s.apiVersion ==== apiVersionValue
			s.requestTimeoutMs ==== RequestTimeoutMsDefault
			s.theme ==== None
			s.defaultLanguage ==== DefaultLanguageDefault
			s.useSecureVerifyUrl ==== false
			s.useSecureWidgetUrl ==== false
			s.captchaType ==== CaptchaTypeDefault
			s.isApiVersion1 ==== false
    	}

    	"succeed if mandatory configuration only (v1) present" in {
    		val conf = Configuration.from(mandatoryV1Config)

			val s = new RecaptchaSettings(conf)
			s.privateKey ==== privateKeyValue
			s.publicKey ==== publicKeyValue
			s.apiVersion ==== v1ApiVersionValue
			s.requestTimeoutMs ==== RequestTimeoutMsDefault
			s.theme ==== None
			s.defaultLanguage ==== DefaultLanguageDefault
			s.useSecureVerifyUrl ==== false
			s.useSecureWidgetUrl ==== false
			s.captchaType ==== CaptchaTypeDefault
			s.isApiVersion1 ==== true
    	}

    	"succeed if all possible v1 configuration present" >> {
    		val conf = Configuration.from(mandatoryV1Config ++ Map(
    				RequestTimeoutConfigProp -> requestTimeoutValueStr,
    				ThemeConfigProp -> v1ThemeValue,
    				DefaultLanguageConfigProp -> defaultLanguageValue,
    				UseSecureWidgetUrlConfigProp -> useSecureWidgetUrlValue,
    				UseSecureVerifyUrlConfigProp -> useSecureVerifyUrlValue
    				))

			val s = new RecaptchaSettings(conf)
			s.privateKey ==== privateKeyValue
			s.publicKey ==== publicKeyValue
			s.apiVersion ==== v1ApiVersionValue
			s.requestTimeoutMs ==== requestTimeoutValue.toMillis
			s.theme ==== Some(v1ThemeValue)
			s.defaultLanguage ==== defaultLanguageValue
			s.useSecureVerifyUrl ==== useSecureVerifyUrlValue
			s.useSecureWidgetUrl ==== useSecureWidgetUrlValue
			s.captchaType ==== CaptchaTypeDefault
			s.isApiVersion1 ==== true
    	}

    	"succeed if all possible v2 configuration present" >> {
    		val conf = Configuration.from(mandatoryV2Config ++ Map(
    				RequestTimeoutConfigProp -> requestTimeoutValueStr,
    				ThemeConfigProp -> v2ThemeValue,
    				CaptchaTypeConfigProp -> captchaTypeValue,
    				CaptchaSizeConfigProp -> captchaSizeValue,
    				LanguageModeConfigProp -> languageModeValue,
    				ForceLanguageConfigProp -> forceLanguageValue
    				))

			val s = new RecaptchaSettings(conf)
			s.privateKey ==== privateKeyValue
			s.publicKey ==== publicKeyValue
			s.apiVersion ==== apiVersionValue
			s.requestTimeoutMs ==== requestTimeoutValue.toMillis
			s.theme ==== Some(v2ThemeValue)
			s.defaultLanguage ==== DefaultLanguageDefault
			s.useSecureVerifyUrl ==== false
			s.useSecureWidgetUrl ==== false
			s.captchaType ==== captchaTypeValue
			s.captchaSize === captchaSizeValue
			s.languageMode === languageModeValue
			s.forceLanguage === Some(forceLanguageValue)
			s.isApiVersion1 ==== false
    	}

    	"fail if no configuration" >> {
    		val conf = Configuration.from(Map())

    		new RecaptchaSettings(conf) must throwAn[ConfigException]
    	}

    	"succeed if booleans are true or false" >> {
    		val conf = Configuration.from(mandatoryV1Config ++ Map(
    				UseSecureWidgetUrlConfigProp -> "true",
    				UseSecureVerifyUrlConfigProp -> "false"
    				))

			val s = new RecaptchaSettings(conf)
			s.useSecureVerifyUrl ==== false
			s.useSecureWidgetUrl ==== true
    	}

    	"succeed if booleans are yes or no" >> {
    		val conf = Configuration.from(mandatoryV1Config ++ Map(
    				UseSecureWidgetUrlConfigProp -> "yes",
    				UseSecureVerifyUrlConfigProp -> "no"
    				))

			val s = new RecaptchaSettings(conf)
			s.useSecureVerifyUrl ==== false
			s.useSecureWidgetUrl ==== true
    	}

    	"fail if booleans are invalid" >> {
    		val conf = Configuration.from(mandatoryV1Config ++ Map(
    				UseSecureWidgetUrlConfigProp -> "wibble",
    				UseSecureVerifyUrlConfigProp -> "abc"
    				))

			new RecaptchaSettings(conf) must throwAn[PlayException]
    	}

    	"fail if private key is missing" >> {
    		val conf = Configuration.from(mandatoryV2Config - PrivateKeyConfigProp)

    		new RecaptchaSettings(conf) must throwAn[ConfigException]
    	}

    	"fail if public key is missing" >> {
    		val conf = Configuration.from(mandatoryV2Config - PublicKeyConfigProp)

    		new RecaptchaSettings(conf) must throwAn[ConfigException]
    	}

    	"fail if api version is missing" >> {
    		val conf = Configuration.from(mandatoryV2Config - ApiVersionConfigProp)

    		new RecaptchaSettings(conf) must throwAn[ConfigException]
    	}

    	"fail if api version is not one of allowed numbers" >> {
    		val conf = Configuration.from(mandatoryV2Config + (ApiVersionConfigProp -> 3))

    		new RecaptchaSettings(conf) must throwAn[ConfigException]
    	}

    	"fail if requestTimeout config value can not parsed as a valid duration" >> {
    		val conf = Configuration.from(mandatoryV2Config + (RequestTimeoutConfigProp -> "10 million dollars"))

    		new RecaptchaSettings(conf) must throwAn[PlayException]
    	}

    	"fail if defaultLanguage config value is not one of allowed languages" >> {
    		val conf = Configuration.from(mandatoryV2Config + (DefaultLanguageConfigProp -> "lsdasdf"))

    		new RecaptchaSettings(conf) must throwAn[PlayException]
    	}

    	"fail if captcha type is not one of allowed values" >> {
    		val conf = Configuration.from(mandatoryV2Config + (CaptchaTypeConfigProp -> "movie"))

    		new RecaptchaSettings(conf) must throwAn[PlayException]
    	}

    	"fail if captcha size is not one of allowed values" >> {
    		val conf = Configuration.from(mandatoryV2Config + (CaptchaSizeConfigProp -> "wibble"))

    		new RecaptchaSettings(conf) must throwAn[PlayException]
    	}

    	"fail if languageMode is force but forceLanguage not set" >> {
    		val conf = Configuration.from(mandatoryV2Config + (LanguageModeConfigProp -> "force"))

    		new RecaptchaSettings(conf) must throwAn[ConfigException]
    	}
    }

    "Recaptcha Settings verifyUrl" should {
    	"return an insecure api v1 url" >> {
    		val conf = Configuration.from(mandatoryV1Config)

    		new RecaptchaSettings(conf).verifyUrl ==== "http://www.google.com/recaptcha/api/verify"
    	}

    	"return a secure api v1 url" >> {
    		val conf = Configuration.from(mandatoryV1Config ++ Map(
    				UseSecureVerifyUrlConfigProp -> true
    				))

    		new RecaptchaSettings(conf).verifyUrl ==== "https://www.google.com/recaptcha/api/verify"
    	}

    	"return a secure api v2 url" >> {
    		val conf = Configuration.from(mandatoryV2Config)

    		new RecaptchaSettings(conf).verifyUrl ==== "https://www.google.com/recaptcha/api/siteverify"
    	}
    }

    "Recaptcha Settings widgetScriptUrl" should {
    	"return an insecure api v1 url" >> {
    		val conf = Configuration.from(mandatoryV1Config)

    		new RecaptchaSettings(conf).widgetScriptUrl ==== "http://www.google.com/recaptcha/api/challenge"
    	}

    	"return a secure api v1 url" >> {
    		val conf = Configuration.from(mandatoryV1Config ++ Map(
    				UseSecureWidgetUrlConfigProp -> true
    				))

    		new RecaptchaSettings(conf).widgetScriptUrl ==== "https://www.google.com/recaptcha/api/challenge"
    	}

    	"return a secure api v2 url" >> {
    		val conf = Configuration.from(mandatoryV2Config)

    		new RecaptchaSettings(conf).widgetScriptUrl ==== "https://www.google.com/recaptcha/api.js"
    	}
    }

    "Recaptcha Settings widgetNoScriptUrl" should {
    	"return an insecure api v1 url" >> {
    		val conf = Configuration.from(mandatoryV1Config)

    		new RecaptchaSettings(conf).widgetNoScriptUrl ==== "http://www.google.com/recaptcha/api/noscript"
    	}

    	"return a secure api v1 url" >> {
    		val conf = Configuration.from(mandatoryV1Config ++ Map(
    				UseSecureWidgetUrlConfigProp -> useSecureWidgetUrlValue
    				))

    		new RecaptchaSettings(conf).widgetNoScriptUrl ==== "https://www.google.com/recaptcha/api/noscript"
    	}

    	"return a secure api v2 url" >> {
    		val conf = Configuration.from(mandatoryV2Config)

    		new RecaptchaSettings(conf).widgetNoScriptUrl ==== "https://www.google.com/recaptcha/api/fallback"
    	}
    }
}
