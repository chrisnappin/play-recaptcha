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

    val requestTimeoutValueStr = "100 seconds"
    val requestTimeoutValue = 100.seconds

    // V2
    val captchaThemeValue = "dark"
    val captchaTypeValue = "audio"
    val captchaSizeValue = "compact"
    val languageModeValue = "force"
    val forceLanguageValue = "ru"
    val mandatoryV2Config = Map (
		PrivateKeyConfigProp -> privateKeyValue,
		PublicKeyConfigProp -> publicKeyValue)

    "Construction of RecaptchaSettings" should {
    	"succeed if mandatory configuration only (v2) present" in {
    		val conf = Configuration.from(mandatoryV2Config)

			val s = new RecaptchaSettings(conf)
			s.privateKey ==== privateKeyValue
			s.publicKey ==== publicKeyValue
			s.requestTimeoutMs ==== RequestTimeoutMsDefault
			s.theme ==== None
			s.captchaType ==== CaptchaTypeDefault
    	}

    	"succeed if all possible v2 configuration present" >> {
    		val conf = Configuration.from(mandatoryV2Config ++ Map(
    				RequestTimeoutConfigProp -> requestTimeoutValueStr,
    				ThemeConfigProp -> captchaThemeValue,
    				CaptchaTypeConfigProp -> captchaTypeValue,
    				CaptchaSizeConfigProp -> captchaSizeValue,
    				LanguageModeConfigProp -> languageModeValue,
    				ForceLanguageConfigProp -> forceLanguageValue
    				))

			val s = new RecaptchaSettings(conf)
			s.privateKey ==== privateKeyValue
			s.publicKey ==== publicKeyValue
			s.requestTimeoutMs ==== requestTimeoutValue.toMillis
			s.theme ==== Some(captchaThemeValue)
			s.captchaType ==== captchaTypeValue
			s.captchaSize === captchaSizeValue
			s.languageMode === languageModeValue
			s.forceLanguage === Some(forceLanguageValue)
    	}

    	"fail if no configuration" >> {
    		val conf = Configuration.from(Map())

    		new RecaptchaSettings(conf) must throwAn[ConfigException]
    	}

    	"fail if private key is missing" >> {
    		val conf = Configuration.from(mandatoryV2Config - PrivateKeyConfigProp)

    		new RecaptchaSettings(conf) must throwAn[ConfigException]
    	}

    	"fail if public key is missing" >> {
    		val conf = Configuration.from(mandatoryV2Config - PublicKeyConfigProp)

    		new RecaptchaSettings(conf) must throwAn[ConfigException]
    	}

    	"fail if requestTimeout config value can not parsed as a valid duration" >> {
    		val conf = Configuration.from(mandatoryV2Config + (RequestTimeoutConfigProp -> "10 million dollars"))

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

    "Recaptcha Settings widgetScriptUrl" should {
    	"return a secure api v2 url" >> {
    		val conf = Configuration.from(mandatoryV2Config)

    		new RecaptchaSettings(conf).widgetScriptUrl ==== "https://www.google.com/recaptcha/api.js"
    	}
    }

    "Recaptcha Settings widgetNoScriptUrl" should {
    	"return a secure api v2 url" >> {
    		val conf = Configuration.from(mandatoryV2Config)

    		new RecaptchaSettings(conf).widgetNoScriptUrl ==== "https://www.google.com/recaptcha/api/fallback"
    	}
    }
}
