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

import java.io.IOException

import RecaptchaSettings.{PrivateKeyConfigProp, PublicKeyConfigProp, RequestTimeoutConfigProp}

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import org.specs2.execute.Result
import org.specs2.mutable.*
import play.api.Configuration
import play.api.data.{Form, FormError}
import play.api.data.Forms.*
import play.api.http.MimeTypes
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws
import play.api.libs.ws.{BodyWritable, WSClient, WSRequest, WSResponse}
import play.api.mvc.{AnyContent, Request}
import play.api.test.{FakeRequest, PlaySpecification}

import scala.concurrent.Future
import scala.concurrent.duration.*
import scala.util.{Left, Right}
import scala.concurrent.ExecutionContext.Implicits.global

/** Tests the <code>RecaptchaVerifier</code> class.
  *
  * @author
  *   chrisnappin
  */
class RecaptchaVerifierSpec extends PlaySpecification:

  val privateKey = "private-key"

  val validV2Settings: Map[String, Any] = Map(
    PrivateKeyConfigProp -> privateKey,
    PublicKeyConfigProp -> "public-key",
    RequestTimeoutConfigProp -> "5 seconds"
  )

  "(v2) RecaptchaVerifier (low level API)" should:

    "handle a valid response as a success" in:

      val (verifier, mockRequest) =
        createMocks(
          validV2Settings,
          OK,
          Some(Json.parse("{\"success\":true}"), Right(Success())),
          false
        )

      await(verifier.verifyV2("aaa", "bbb")) must beRight(Success())
      checkRecaptchaV2Request(mockRequest, "aaa", "bbb", privateKey)

    "handle a parser error as an error" in:
      val (verifier, mockRequest) =
        createMocks(
          validV2Settings,
          OK,
          Some(Json.parse("{}"), Left(Error(RecaptchaErrorCode.apiError))),
          false
        )

      await(verifier.verifyV2("aaa", "bbb")) must beLeft(Error(RecaptchaErrorCode.apiError))
      checkRecaptchaV2Request(mockRequest, "aaa", "bbb", privateKey)

    "handle a 404 response as an error" in:
      val (verifier, mockRequest) = createMocks(validV2Settings, NOT_FOUND, None, false)

      await(verifier.verifyV2("aaa", "bbb")) must beLeft(
        Error(RecaptchaErrorCode.recaptchaNotReachable)
      )
      checkRecaptchaV2Request(mockRequest, "aaa", "bbb", privateKey)

    "handle an IOException as an error" in:
      val (verifier, mockRequest) = createMocks(validV2Settings, OK, None, true)

      await(verifier.verifyV2("aaa", "bbb")) must beLeft(
        Error(RecaptchaErrorCode.recaptchaNotReachable)
      )
      checkRecaptchaV2Request(mockRequest, "aaa", "bbb", privateKey)

  // used to bind with
  case class Model(field1: String, field2: Option[Int])
  object Model:
    def unapply(m: Model): Option[(String, Option[Int])] = Some(m.field1, m.field2)

  val modelForm = Form(
    mapping(
      "field1" -> nonEmptyText,
      "field2" -> optional(number)
    )(Model.apply)(Model.unapply)
  )

  private val context = scala.concurrent.ExecutionContext.Implicits.global

  "RecaptchaVerifier (high level API) - form URL encoded body" should:

    "stop at a binding error and not call recaptcha" in:
      highLevelApiBindingError(
        FakeRequest()
          .withFormUrlEncodedBody(
            "field2" -> "aaa", // not a number
            RecaptchaVerifier.recaptchaV2ResponseField -> "r"
          )
          .withHeaders(CONTENT_TYPE -> MimeTypes.FORM)
      )

    "throw exception if no recaptcha response" in:
      highLevelApiNoRecaptchaResponse(
        FakeRequest()
          .withFormUrlEncodedBody(
            "field1" -> "aaa"
            // no recaptcha response
          )
          .withHeaders(CONTENT_TYPE -> MimeTypes.FORM)
      )

    "throw exception if multiple recaptcha responses" in:
      highLevelApiMultipleRecaptchaResponses(
        FakeRequest()
          .withFormUrlEncodedBody(
            "field1" -> "aaa",
            RecaptchaVerifier.recaptchaV2ResponseField -> "r1",
            RecaptchaVerifier.recaptchaV2ResponseField -> "r2" // multiple
          )
          .withHeaders(CONTENT_TYPE -> MimeTypes.FORM)
      )

    "return form error if empty recaptcha response" in:
      highLevelApiEmptyRecaptchaResponse(
        FakeRequest()
          .withFormUrlEncodedBody(
            "field1" -> "aaa",
            RecaptchaVerifier.recaptchaV2ResponseField -> "" // empty response
          )
          .withHeaders(CONTENT_TYPE -> MimeTypes.FORM)
      )

    "return form errors if empty recaptcha response and form bind error" in:
      highLevelApiEmptyRecaptchaResponseAndBindError(
        FakeRequest()
          .withFormUrlEncodedBody(
            "field2" -> "aaa", // not a number
            RecaptchaVerifier.recaptchaV2ResponseField -> "" // response missing
          )
          .withHeaders(CONTENT_TYPE -> MimeTypes.FORM)
      )

    "return form error if recaptcha error" in:
      highLevelApiRecaptchaError(
        FakeRequest()
          .withFormUrlEncodedBody(
            "field1" -> "aaa",
            RecaptchaVerifier.recaptchaV2ResponseField -> "r"
          )
          .withHeaders(CONTENT_TYPE -> MimeTypes.FORM)
      )

    "return form if recaptcha is successful" in:
      highLevelApiRecaptchaValid(
        FakeRequest()
          .withFormUrlEncodedBody(
            "field1" -> "aaa",
            RecaptchaVerifier.recaptchaV2ResponseField -> "r"
          )
          .withHeaders(CONTENT_TYPE -> MimeTypes.FORM)
      )

  "RecaptchaVerifier (high level API) - JSON body" should:

    "stop at a binding error and not call recaptcha" in:
      highLevelApiBindingError(
        FakeRequest()
          .withJsonBody(
            Json.obj(
              "field2" -> "aaa", // not a number
              RecaptchaVerifier.recaptchaV2ResponseField -> "r"
            )
          )
          .withHeaders(CONTENT_TYPE -> MimeTypes.JSON)
      )

    "throw exception if no recaptcha response" in:
      highLevelApiNoRecaptchaResponse(
        FakeRequest()
          .withJsonBody(
            Json.obj(
              "field1" -> "aaa"
              // no recaptcha response
            )
          )
          .withHeaders(CONTENT_TYPE -> MimeTypes.JSON)
      )

    // Note: not testing multiple recaptcha responses, since the JSON to parameter map doesn't support this

    "return form error if empty recaptcha response" in:
      highLevelApiEmptyRecaptchaResponse(
        FakeRequest()
          .withJsonBody(
            Json.obj(
              "field1" -> "aaa",
              RecaptchaVerifier.recaptchaV2ResponseField -> "" // value empty
            )
          )
          .withHeaders(CONTENT_TYPE -> MimeTypes.JSON)
      )

    "return form errors if empty recaptcha response and form bind error" in:
      highLevelApiEmptyRecaptchaResponseAndBindError(
        FakeRequest()
          .withJsonBody(
            Json.obj(
              "field2" -> "aaa", // not a number
              RecaptchaVerifier.recaptchaV2ResponseField -> "" // response missing
            )
          )
          .withHeaders(CONTENT_TYPE -> MimeTypes.JSON)
      )

    "return form error if recaptcha error" in:
      highLevelApiRecaptchaError(
        FakeRequest()
          .withJsonBody(
            Json.obj(
              "field1" -> "aaa",
              RecaptchaVerifier.recaptchaV2ResponseField -> "r"
            )
          )
          .withHeaders(CONTENT_TYPE -> MimeTypes.JSON)
      )

    "return form if recaptcha is successful" in:
      highLevelApiRecaptchaValid(
        FakeRequest()
          .withJsonBody(
            Json.obj(
              "field1" -> "aaa",
              RecaptchaVerifier.recaptchaV2ResponseField -> "r"
            )
          )
          .withHeaders(CONTENT_TYPE -> MimeTypes.JSON)
      )

  /** Tests that the high level API stops at a binding error and doesn't invoke recaptcha.
    *
    * @param request
    *   The request
    * @return
    *   The test result
    */
  private def highLevelApiBindingError(request: Request[AnyContent]): Result =
    val (verifier, mockRequest) = createMocks(validV2Settings, OK, None, false)

    val result = await(verifier.bindFromRequestAndVerify(modelForm)(request, context))

    result.hasErrors must equalTo(true)
    result.error("field2") must beSome(FormError("field2", "error.number"))
    checkRecaptchaNotInvoked(mockRequest)

  /** Tests that the high level API throws an exception if request contains no recaptcha response.
    *
    * @param request
    *   The request
    * @return
    *   The test result
    */
  private def highLevelApiNoRecaptchaResponse(request: Request[AnyContent]): Result =
    val (verifier, mockRequest) = createMocks(validV2Settings, OK, None, false)

    await(verifier.bindFromRequestAndVerify(modelForm)(request, context)) must throwA[
      IllegalStateException
    ]
    checkRecaptchaNotInvoked(mockRequest)

  /** Tests that the high level API throws an exception if request contains multiple recaptcha
    * responses.
    *
    * @param request
    *   The request
    * @return
    *   The test result
    */
  private def highLevelApiMultipleRecaptchaResponses(request: Request[AnyContent]): Result =
    val (verifier, mockRequest) = createMocks(validV2Settings, OK, None, false)

    await(verifier.bindFromRequestAndVerify(modelForm)(request, context)) must throwA[
      IllegalStateException
    ]
    checkRecaptchaNotInvoked(mockRequest)

  /** Tests that the high level API returns a form error if the recaptcha response is empty.
    *
    * @param request
    *   The request
    * @return
    *   The test result
    */
  private def highLevelApiEmptyRecaptchaResponse(request: Request[AnyContent]): Result =
    val (verifier, mockRequest) = createMocks(validV2Settings, OK, None, false)

    val result = await(verifier.bindFromRequestAndVerify(modelForm)(request, context))

    result.hasErrors must equalTo(true)
    result.error(RecaptchaVerifier.formErrorKey) must
      beSome(FormError(RecaptchaVerifier.formErrorKey, RecaptchaErrorCode.responseMissing))
    checkRecaptchaNotInvoked(mockRequest)

  /** Tests that the high level API returns multiple form errors if the recaptcha response is empty
    * and there are also other bind errors.
    *
    * @param request
    *   The request
    * @return
    *   The test result
    */
  private def highLevelApiEmptyRecaptchaResponseAndBindError(
      request: Request[AnyContent]
  ): Result =
    val (verifier, mockRequest) = createMocks(validV2Settings, OK, None, false)

    val result = await(verifier.bindFromRequestAndVerify(modelForm)(request, context))

    result.hasErrors must equalTo(true)
    result.error("field2") must beSome(FormError("field2", "error.number"))
    result.error(RecaptchaVerifier.formErrorKey) must
      beSome(FormError(RecaptchaVerifier.formErrorKey, RecaptchaErrorCode.responseMissing))
    checkRecaptchaNotInvoked(mockRequest)

  /** Tests that the high level API returns a form error if recaptcha verification fails.
    *
    * @param request
    *   The request
    * @return
    *   The test result
    */
  private def highLevelApiRecaptchaError(request: Request[AnyContent]): Result =
    val (verifier, mockRequest) = createMocks(
      validV2Settings,
      OK,
      Some(Json.parse("{}"), Left(Error(RecaptchaErrorCode.captchaIncorrect))),
      false
    )

    val result = await(verifier.bindFromRequestAndVerify(modelForm)(request, context))

    result.hasErrors must equalTo(true)
    result.error(RecaptchaVerifier.formErrorKey) must
      beSome(FormError(RecaptchaVerifier.formErrorKey, RecaptchaErrorCode.captchaIncorrect))
    checkRecaptchaV2Request(mockRequest, "r", "127.0.0.1", privateKey)

  /** Tests that the high level API returns a form if recaptcha verification succeeds.
    *
    * @param request
    *   The request
    * @return
    *   The test result
    */
  private def highLevelApiRecaptchaValid(request: Request[AnyContent]): Result =
    val (verifier, mockRequest) =
      createMocks(validV2Settings, OK, Some(Json.parse("{}"), Right(Success())), false)

    val result = await(verifier.bindFromRequestAndVerify(modelForm)(request, context))

    result.hasErrors must equalTo(false)
    checkRecaptchaV2Request(mockRequest, "r", "127.0.0.1", privateKey)

  /** Creates a verifier wired up with mocked dependencies.
    *
    * @param configProps
    *   App config from which settings will be extracted
    * @param recaptchaResponseCode
    *   The HTTP response code that recaptcha should return
    * @param v2bodyAndParserResult
    *   If set, the v2 response body and parser result to use
    * @param futureThrowsError
    *   Whether the web service future should throw an IOException
    * @return
    *   The verifier and the web service request
    */
  private def createMocks(
      configProps: Map[String, Any],
      recaptchaResponseCode: Int,
      v2bodyAndParserResult: Option[(JsValue, Either[Error, Success])],
      futureThrowsError: Boolean
  ): (RecaptchaVerifier, WSRequest) =
    val conf = Configuration.from(configProps)

    val settings = new RecaptchaSettings(conf)

    val mockWSClient = mock(classOf[WSClient])
    val mockRequest = mock(classOf[WSRequest])
    val mockResponse = mock(classOf[WSResponse])
    val futureResponse = Future:
      if futureThrowsError then throw new IOException("Oops") else mockResponse
    val mockParser = mock(classOf[ResponseParser])

    when(mockWSClient.url(settings.verifyUrl)).thenReturn(mockRequest)
    when(mockRequest.withRequestTimeout(5.seconds)).thenReturn(mockRequest)
    when(
      mockRequest.post(any[Map[String, Seq[String]]])(any[BodyWritable[Map[String, Seq[String]]]])
    ).thenReturn(futureResponse)

    when(mockResponse.status).thenReturn(recaptchaResponseCode)

    // mock the API v2 parser input and output
    v2bodyAndParserResult.map { case (body, parserResult) =>
      when(mockResponse.json).thenReturn(body)
      when(mockParser.parseV2Response(body)).thenReturn(parserResult)
    }

    val verifier = new RecaptchaVerifier(settings, mockParser, mockWSClient)

    (verifier, mockRequest)

  /** Checks the API v2 request sent to recaptcha.
    *
    * @param request
    *   The mock request to check
    * @param response
    *   The recaptcha response
    * @param remoteIP
    *   The remote IP
    * @param privateKey
    *   The recaptcha private key
    */
  private def checkRecaptchaV2Request(
      request: WSRequest,
      response: String,
      remoteIP: String,
      privateKey: String
  ): Result =
    val captor = ArgumentCaptor.forClass(classOf[Map[String, Seq[String]]])
    verify(request).post(captor.capture())(any[BodyWritable[Map[String, Seq[String]]]])
    val argument = captor.getValue
    argument("response") must equalTo(Seq(response))
    argument("remoteip") must equalTo(Seq(remoteIP))
    argument("secret") must equalTo(Seq(privateKey))

  /** Checks that the recaptcha request was never sent.
    *
    * @param request
    *   The mock request to check
    */
  private def checkRecaptchaNotInvoked(request: WSRequest): Result =
    verify(request, never())
      .post(any(classOf[Map[String, Seq[String]]]))(any[BodyWritable[Map[String, Seq[String]]]])
    true must equalTo(true) // produce a result
