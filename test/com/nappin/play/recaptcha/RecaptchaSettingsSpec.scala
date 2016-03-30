package com.nappin.play.recaptcha

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.{PlayException, Configuration}
import com.typesafe.config.ConfigException

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

  // V2
  val v2ThemeValue = "dark"
  val captchaTypeValue = "audio"

  val defaultConfigProps = Map (
    PrivateKeyConfigProp -> privateKeyValue,
    PublicKeyConfigProp -> publicKeyValue,
    ApiVersionConfigProp -> apiVersionValue
  )


  "Construction of RecaptchaSettings" should {
    "succeed if all required fields are filled in" >> {
      val conf = Configuration.from(defaultConfigProps)

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

    "succeed with all required and v1 optional fields filled in" >> {
      val conf = Configuration.from((defaultConfigProps + (ApiVersionConfigProp -> v1ApiVersionValue)) ++ Map(
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

    "succeed with all required and v2 optional fields filled in" >> {
      val conf = Configuration.from(defaultConfigProps ++ Map(
        RequestTimeoutConfigProp -> requestTimeoutValueStr,
        ThemeConfigProp -> v2ThemeValue,
        CaptchaTypeConfigProp -> captchaTypeValue
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
      s.isApiVersion1 ==== false
    }

    "fail if private key is missing" >> {
      val conf = Configuration.from(defaultConfigProps - PrivateKeyConfigProp)

      new RecaptchaSettings(conf) must throwAn[ConfigException]
    }

    "fail if public key is missing" >> {
      val conf = Configuration.from(defaultConfigProps - PublicKeyConfigProp)

      new RecaptchaSettings(conf) must throwAn[ConfigException]
    }

    "fail if api version is missing" >> {
      val conf = Configuration.from(defaultConfigProps - ApiVersionConfigProp)

      new RecaptchaSettings(conf) must throwAn[ConfigException]
    }

    "fail if api version is not one of allowed numbers" >> {
      val conf = Configuration.from(defaultConfigProps + (ApiVersionConfigProp -> 3))

      new RecaptchaSettings(conf) must throwAn[PlayException]
    }

    "fail if requestTimeout config value can not parsed as a valid duration" >> {
      val conf = Configuration.from(defaultConfigProps + (RequestTimeoutConfigProp -> "10 million dollars"))

      new RecaptchaSettings(conf) must throwAn[PlayException]
    }

    "fail if defaultLanguage config value is not one of allowed languages" >> {
      val conf = Configuration.from(defaultConfigProps + (DefaultLanguageConfigProp -> "lsdasdf"))

      new RecaptchaSettings(conf) must throwAn[PlayException]
    }

    "fail if captcha type is not one of allowed values" >> {
      val conf = Configuration.from(defaultConfigProps + (CaptchaTypeConfigProp -> "movie"))

      new RecaptchaSettings(conf) must throwAn[PlayException]
    }
  }

  "Recaptcha Settings verifyUrl" should {
    "return an insecure api v1 url" >> {
      val conf = Configuration.from(defaultConfigProps ++ Map(
        ApiVersionConfigProp -> v1ApiVersionValue
      ))

      new RecaptchaSettings(conf).verifyUrl ==== "http://www.google.com/recaptcha/api/verify"
    }

    "return a secure api v1 url" >> {
      val conf = Configuration.from(defaultConfigProps ++ Map(
        ApiVersionConfigProp -> v1ApiVersionValue,
        UseSecureVerifyUrlConfigProp -> true
      ))

      new RecaptchaSettings(conf).verifyUrl ==== "https://www.google.com/recaptcha/api/verify"
    }

    "return a secure api v2 url" >> {
      val conf = Configuration.from(defaultConfigProps)

      new RecaptchaSettings(conf).verifyUrl ==== "https://www.google.com/recaptcha/api/siteverify"
    }
  }

  "Recaptcha Settings widgetScriptUrl" should {
    "return an insecure api v1 url" >> {
      val conf = Configuration.from(defaultConfigProps ++ Map(
        ApiVersionConfigProp -> v1ApiVersionValue
      ))

      new RecaptchaSettings(conf).widgetScriptUrl ==== "http://www.google.com/recaptcha/api/challenge"
    }

    "return a secure api v1 url" >> {
      val conf = Configuration.from(defaultConfigProps ++ Map(
        ApiVersionConfigProp -> v1ApiVersionValue,
        UseSecureWidgetUrlConfigProp -> true
      ))

      new RecaptchaSettings(conf).widgetScriptUrl ==== "https://www.google.com/recaptcha/api/challenge"
    }

    "return a secure api v2 url" >> {
      val conf = Configuration.from(defaultConfigProps)

      new RecaptchaSettings(conf).widgetScriptUrl ==== "https://www.google.com/recaptcha/api.js"
    }
  }

  "Recaptcha Settings widgetNoScriptUrl" should {
    "return an insecure api v1 url" >> {
      val conf = Configuration.from(defaultConfigProps ++ Map(
        ApiVersionConfigProp -> v1ApiVersionValue
      ))

      new RecaptchaSettings(conf).widgetNoScriptUrl ==== "http://www.google.com/recaptcha/api/noscript"
    }

    "return a secure api v1 url" >> {
      val conf = Configuration.from(defaultConfigProps ++ Map(
        ApiVersionConfigProp -> v1ApiVersionValue,
        UseSecureWidgetUrlConfigProp -> useSecureWidgetUrlValue
      ))

      new RecaptchaSettings(conf).widgetNoScriptUrl ==== "https://www.google.com/recaptcha/api/noscript"
    }

    "return a secure api v2 url" >> {
      val conf = Configuration.from(defaultConfigProps)

      new RecaptchaSettings(conf).widgetNoScriptUrl ==== "https://www.google.com/recaptcha/api/fallback"
    }
  }
}
