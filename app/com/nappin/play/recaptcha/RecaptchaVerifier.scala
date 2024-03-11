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

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.mvc.{AnyContent, Request}
import play.api.data.Form
import play.api.data.FormBinding.Implicits.formBinding
import play.api.libs.ws
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import play.api.libs.json.*

object RecaptchaVerifier {

  /** The artificial form field key used for captcha errors. */
  val formErrorKey = "com.nappin.play.recaptcha.error"

  /** The recaptcha challenge field name. */
  val recaptchaChallengeField = "recaptcha_challenge_field"

  /** The recaptcha (v2) response field name. */
  val recaptchaV2ResponseField = "g-recaptcha-response"
}

/** Verifies whether a recaptcha response is valid, by invoking the Google Recaptcha verify web
  * service.
  *
  * @author
  *   chrisnappin, AmazingDreams
  * @constructor
  *   Creates a new instance.
  * @param settings
  *   The Recaptcha settings
  * @param parser
  *   The response parser to use
  * @param wsClient
  *   The web service client to use
  */
@Singleton
class RecaptchaVerifier @Inject() (
    settings: RecaptchaSettings,
    parser: ResponseParser,
    wsClient: WSClient
) {

  private val logger = Logger(this.getClass)

  /** Get the recaptcha data from the request, regardless of the format (form, JSON, etc).
    *
    * Returns an empty map if an unsupported request format.
    *
    * @param request
    *   The implicit request
    * @return
    *   A map of request parameter values, keyed by parameter name
    */
  private def getRecaptchaPostData()(implicit
      request: play.api.mvc.Request[?]
  ): Map[String, Seq[String]] = {
    request.body match {
      case body: play.api.mvc.AnyContent if body.asFormUrlEncoded.isDefined =>
        body.asFormUrlEncoded.get.filter(data =>
          data._1 == RecaptchaVerifier.recaptchaV2ResponseField
        )

      case body: play.api.mvc.AnyContent if body.asMultipartFormData.isDefined =>
        body.asMultipartFormData.get.asFormUrlEncoded.filter(data =>
          data._1 == RecaptchaVerifier.recaptchaV2ResponseField
        )

      case body: play.api.mvc.AnyContent if body.asJson.isDefined =>
        val recaptchaResponse = body.asJson.get \ RecaptchaVerifier.recaptchaV2ResponseField
        if recaptchaResponse.isDefined then
          Map(RecaptchaVerifier.recaptchaV2ResponseField -> Seq(recaptchaResponse.as[String]))
        else Map.empty[String, Seq[String]]

      case body: Map[?, ?] =>
        body
          .asInstanceOf[Map[String, Seq[String]]]
          .filter(data => data._1 == RecaptchaVerifier.recaptchaV2ResponseField)

      case body: play.api.mvc.MultipartFormData[?] =>
        body.asFormUrlEncoded.filter(data => data._1 == RecaptchaVerifier.recaptchaV2ResponseField)

      case body: play.api.libs.json.JsValue =>
        val recaptchaResponse = body \ RecaptchaVerifier.recaptchaV2ResponseField
        if recaptchaResponse.isDefined then
          Map(RecaptchaVerifier.recaptchaV2ResponseField -> Seq(recaptchaResponse.as[String]))
        else Map.empty[String, Seq[String]]

      case _ =>
        Map.empty[String, Seq[String]]

    }
  }

  /** High level API (using Play forms).
    *
    * Binds form data from the request, and if valid then verifies the recaptcha response, by
    * invoking the Google Recaptcha verify web service (API v2) in a reactive manner. Possible
    * errors include: 
    * <ul> 
    *   <li>Binding error (form validation, regardless of recaptcha)</li>
    *   <li>Recaptcha response missing (end user didn't enter it)</li>
    *   <li>Recpatcha response incorrect</li>
    *   <li>Error invoking the recaptcha service</li>
    * </ul>
    * Apart from generic binding error, the recaptcha errors are populated against the artificial
    * form field key <code>formErrorKey</code>, handled by the recaptcha view template tags.
    *
    * @param form
    *   The form
    * @param request
    *   Implicit - The current web request
    * @param context
    *   Implicit - The execution context used for futures
    * @return
    *   A future that will be the form to use, either populated with an error or success
    * @throws IllegalStateException
    *   Developer errors that shouldn't happen - no recaptcha challenge, or multiple challenges or
    *   responses found
    */
  def bindFromRequestAndVerify[T](form: Form[T])(implicit
      request: Request[AnyContent],
      context: ExecutionContext
  ): Future[Form[T]] = {

    val boundForm = form.bindFromRequest()

    val response = readResponse(getRecaptchaPostData())

    if response.length < 1 then {
      // probably an end user error
      logger.debug(
        "User did not enter a captcha response in the form POST submitted"
      )

      // return the missing required field, plus any other form bind errors that might have happened
      return Future {
        boundForm.withError(
          RecaptchaVerifier.formErrorKey,
          RecaptchaErrorCode.responseMissing
        )
      }
    }

    boundForm.fold(
      // form binding failed, so don't call recaptcha
      error =>
        Future {
          logger.debug("Binding error")
          error
        },

      // form binding succeeded, so verify the captcha response
      success => {
        val result = verifyV2(response, request.remoteAddress)

        result.map { r =>
          {
            r.fold(
              // captcha incorrect, or a technical error
              error => boundForm.withError(RecaptchaVerifier.formErrorKey, error.code),

              // all success
              success => boundForm
            )
          }
        }
      }
    )
  }

  /** Read the response field from the POST'ed response.
    *
    * @param params
    *   The POST parameters
    * @return
    *   The response
    * @throws IllegalStateException
    *   If response missing or multiple
    */
  private def readResponse(params: Map[String, Seq[String]]): String = {
    val fieldName = RecaptchaVerifier.recaptchaV2ResponseField

    if !params.contains(fieldName) then {
      // probably a developer error
      val message =
        "No recaptcha response POSTed, check the form submitted was valid"
      logger.error(message)
      throw new IllegalStateException(message)

    } else if params(fieldName).size > 1 then {
      // probably a developer error
      val message =
        "Multiple recaptcha responses POSTed, check the form submitted was valid"
      logger.error(message)
      throw new IllegalStateException(message)
    }
    params(fieldName).head
  }

  /** Low level API (independent of Play form and request APIs).
    *
    * Verifies whether a recaptcha response is valid, by invoking the Google Recaptcha API version 2
    * verify web service in a reactive manner.
    *
    * @param response
    *   The recaptcha response, to verify
    * @param remoteIp
    *   The IP address of the end user
    * @param context
    *   Implicit - The execution context used for futures
    * @return
    *   A future that will be either an Error (with a code) or Success
    */
  def verifyV2(response: String, remoteIp: String)(implicit
      context: ExecutionContext
  ): Future[Either[Error, Success]] = {

    // create the v2 POST payload
    val payload = Map(
      "secret" -> Seq(settings.privateKey),
      "response" -> Seq(response),
      "remoteip" -> Seq(remoteIp)
    )

    implicit val w = ws.writeableOf_urlEncodedForm

    logger.info(s"Verifying v2 recaptcha ($response) for $remoteIp")
    val futureResponse = wsClient
      .url(settings.verifyUrl)
      .withRequestTimeout(
        Duration(settings.requestTimeoutMs, TimeUnit.MILLISECONDS)
      )
      .post(payload)

    futureResponse.map { response =>
      {
        if response.status == play.api.http.Status.OK then {
          parser.parseV2Response(response.json)

        } else {
          logger.error(
            "Error calling recaptcha v2 API, HTTP response " + response.status
          )
          Left(Error(RecaptchaErrorCode.recaptchaNotReachable))
        }
      }

    } recover {
      case ex: java.io.IOException =>
        logger.error("Unable to call recaptcha v2 API", ex)
        Left(Error(RecaptchaErrorCode.recaptchaNotReachable))

      // e.g. various JSON parsing errors are possible
      case other: Any =>
        logger.error("Error calling recaptcha v2 API: " + other.getMessage)
        Left(Error(RecaptchaErrorCode.apiError))
    }
  }
}

/** Used to hold various recaptcha error codes. Some are defined by Google Recaptcha, some are used
  * solely by this module. Yes, the Google Recpatcha documentation states not to rely on these, so
  * we deliberately keep these to a minimum.
  */
object RecaptchaErrorCode {

  /** Defined in Google Recaptcha documentation, returned by Recaptcha itself, means the captcha was
    * incorrect.
    */
  val captchaIncorrect = "incorrect-captcha-sol"

  /** Defined in Google Recaptcha documentation, used by this module, means recaptcha itself
    * couldn't be reached.
    */
  val recaptchaNotReachable = "recaptcha-not-reachable"

  /** An API error (e.g. invalid format response). */
  val apiError = "recaptcha-api-error"

  /** The recaptcha response was missing from the request (probably an end user error).
    */
  val responseMissing = "recaptcha-response-missing"
}
