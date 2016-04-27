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

import java.io.IOException
import javax.inject.Provider

import org.mockito.ArgumentCaptor
import org.specs2.mock.Mockito
import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith
import play.api.{Configuration, Application}

import play.api.data._
import play.api.data.Forms._
import play.api.http.{MimeTypes, Writeable}
import play.api.libs.json.{Json, JsValue}
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import play.api.test.{FakeApplication, FakeRequest, PlaySpecification, WithApplication}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Left, Right}

/**
 * Tests the <code>RecaptchaVerifier</code> class.
 *
 * @author Chris Nappin
 */
@RunWith(classOf[JUnitRunner])
class RecaptchaVerifierSpec extends PlaySpecification with Mockito {

    val privateKey = "private-key"

    val validV1Application = new FakeApplication(
            additionalConfiguration = Map(
                RecaptchaConfiguration.privateKey -> "private-key",
                RecaptchaConfiguration.publicKey -> "public-key",
                RecaptchaConfiguration.apiVersion -> "1",
                RecaptchaConfiguration.requestTimeout -> "5 seconds"))

    val validV2Application = new FakeApplication(
            additionalConfiguration = Map(
                RecaptchaConfiguration.privateKey -> "private-key",
                RecaptchaConfiguration.publicKey -> "public-key",
                RecaptchaConfiguration.apiVersion -> "2",
                RecaptchaConfiguration.requestTimeout -> "5 seconds"))

    def recaptchaUrls(implicit application: Application) = application.injector.instanceOf[RecaptchaUrls]
    def recaptchaModuleProv(implicit application: Application) = new Provider[RecaptchaModule]() {
        override def get(): RecaptchaModule = application.injector.instanceOf[RecaptchaModule]
    }
    def configuration(implicit application: Application) = application.injector.instanceOf[Configuration]


    /** Has mandatory configuration missing. */
    val invalidApplication = new FakeApplication()

    "(v1) RecaptchaVerifier (low level API)" should {

        "throw exception on construction if configuration invalid" in
                new WithApplication(invalidApplication) {
            createMocks(OK, None, None, false) must throwA[RecaptchaConfigurationException]
        }

        "handle a valid response as a success" in new WithApplication(validV1Application) {
            val (verifier, mockRequest) =
                createMocks(OK, Some("true\nSuccess", Right(Success())), None, false)

            await(verifier.verifyV1("aaa", "bbb", "ccc")) must equalTo(Right(Success()))

            checkRecaptchaV1Request(mockRequest, "aaa", "bbb", "ccc", privateKey)
        }

        "handle a parser error as an error" in new WithApplication(validV1Application) {
            val (verifier, mockRequest) =
                createMocks(OK, Some("invaid response", Left(Error(RecaptchaErrorCode.apiError))),
                        None, false)

            await(verifier.verifyV1("aaa", "bbb", "ccc")) must
                equalTo(Left(Error(RecaptchaErrorCode.apiError)))

            checkRecaptchaV1Request(mockRequest, "aaa", "bbb", "ccc", privateKey)
        }

        "handle a 404 response as an error" in new WithApplication(validV1Application) {
            val (verifier, mockRequest) = createMocks(NOT_FOUND, None, None, false)

            await(verifier.verifyV1("aaa", "bbb", "ccc")) must
                equalTo(Left(Error(RecaptchaErrorCode.recaptchaNotReachable)))

            checkRecaptchaV1Request(mockRequest, "aaa", "bbb", "ccc", privateKey)
        }

        "handle an IOException as an error" in new WithApplication(validV1Application) {
            val (verifier, mockRequest) = createMocks(OK, None, None, true)

            await(verifier.verifyV1("aaa", "bbb", "ccc")) must
                equalTo(Left(Error(RecaptchaErrorCode.recaptchaNotReachable)))

            checkRecaptchaV1Request(mockRequest, "aaa", "bbb", "ccc", privateKey)
        }
    }

    "(v2) RecaptchaVerifier (low level API)" should {

        "throw exception on construction if plugin not enabled" in
                new WithApplication(invalidApplication) {
            createMocks(OK, None, None, false) must throwA[RecaptchaConfigurationException]
        }

        "handle a valid response as a success" in new WithApplication(validV2Application) {
            val (verifier, mockRequest) =
                createMocks(OK, None, Some(Json.parse("{\"success\":true}"), Right(Success())), false)

            await(verifier.verifyV2("aaa", "bbb")) must equalTo(Right(Success()))

            checkRecaptchaV2Request(mockRequest, "aaa", "bbb", privateKey)
        }

        "handle a parser error as an error" in new WithApplication(validV2Application) {
            val (verifier, mockRequest) =
                createMocks(OK, None, Some(Json.parse("{}"), Left(Error(RecaptchaErrorCode.apiError))), false)

            await(verifier.verifyV2("aaa", "bbb")) must
                equalTo(Left(Error(RecaptchaErrorCode.apiError)))

            checkRecaptchaV2Request(mockRequest, "aaa", "bbb", privateKey)
        }

        "handle a 404 response as an error" in new WithApplication(validV2Application) {
            val (verifier, mockRequest) = createMocks(NOT_FOUND, None, None, false)

            await(verifier.verifyV2("aaa", "bbb")) must
                equalTo(Left(Error(RecaptchaErrorCode.recaptchaNotReachable)))

            checkRecaptchaV2Request(mockRequest, "aaa", "bbb", privateKey)
        }

        "handle an IOException as an error" in new WithApplication(validV2Application) {
            val (verifier, mockRequest) = createMocks(OK, None, None, true)

            await(verifier.verifyV2("aaa", "bbb")) must
                equalTo(Left(Error(RecaptchaErrorCode.recaptchaNotReachable)))

            checkRecaptchaV2Request(mockRequest, "aaa", "bbb", privateKey)
        }
    }

    // used to bind with
    case class Model(field1: String, field2: Option[Int])

    val modelForm = Form(mapping(
            "field1" -> nonEmptyText,
            "field2" -> optional(number)
        )(Model.apply)(Model.unapply))

    implicit val context = scala.concurrent.ExecutionContext.Implicits.global


    "(v1) RecaptchaVerifier (high level API)" should {

        "throw exception on construction if plugin not enabled" in
                new WithApplication(invalidApplication) {
            createMocks(OK, None, None, false) must throwA[RecaptchaConfigurationException]
        }

        "stop at a binding error and not call recaptcha" in new WithApplication(validV1Application) {
            val (verifier, mockRequest) = createMocks(OK, None, None, false)
            val request = FakeRequest().withFormUrlEncodedBody(
                    			"field2" -> "aaa", // not a number
                    			RecaptchaVerifier.recaptchaChallengeField -> "c",
                    			RecaptchaVerifier.recaptchaResponseField -> "r"
                    		).withHeaders(CONTENT_TYPE -> MimeTypes.FORM)

            val result = await(verifier.bindFromRequestAndVerify(modelForm)(request, context))

            result.hasErrors must equalTo(true)
            result.error("field2") must equalTo(Some(FormError("field2", "error.number")))
            checkRecaptchaNotInvoked(mockRequest)
        }

        "throw exception if no recaptcha challenge" in new WithApplication(validV1Application) {
            val (verifier, mockRequest) = createMocks(OK, None, None, false)
            val request = FakeRequest().withFormUrlEncodedBody("field1" -> "aaa")
            				.withHeaders(CONTENT_TYPE -> MimeTypes.FORM)

            await(verifier.bindFromRequestAndVerify(modelForm)(request, context)) must
                throwA[IllegalStateException]
            checkRecaptchaNotInvoked(mockRequest)
        }

        "throw exception if empty recaptcha challenge" in new WithApplication(validV1Application) {
            val (verifier, mockRequest) = createMocks(OK, None, None, false)
            val request = FakeRequest().withFormUrlEncodedBody(
                    			"field1" -> "aaa",
                    			RecaptchaVerifier.recaptchaChallengeField -> ""
                    		).withHeaders(CONTENT_TYPE -> MimeTypes.FORM)

            await(verifier.bindFromRequestAndVerify(modelForm)(request, context)) must
                throwA[IllegalStateException]
            checkRecaptchaNotInvoked(mockRequest)
        }

        "throw exception if multiple recaptcha challenges" in new WithApplication(validV1Application) {
            val (verifier, mockRequest) = createMocks(OK, None, None, false)
            val request = FakeRequest().withFormUrlEncodedBody(
                    			"field1" -> "aaa",
                    			RecaptchaVerifier.recaptchaChallengeField -> "c1",
                    			RecaptchaVerifier.recaptchaChallengeField -> "c2"
                    		).withHeaders(CONTENT_TYPE -> MimeTypes.FORM)

            await(verifier.bindFromRequestAndVerify(modelForm)(request, context)) must
                throwA[IllegalStateException]
            checkRecaptchaNotInvoked(mockRequest)
        }

        "throw exception if no recaptcha response" in new WithApplication(validV1Application) {
            val (verifier, mockRequest) = createMocks(OK, None, None, false)
            val request = FakeRequest().withFormUrlEncodedBody(
                    			"field1" -> "aaa",
                    			RecaptchaVerifier.recaptchaChallengeField -> "c"
                    		).withHeaders(CONTENT_TYPE -> MimeTypes.FORM)

            await(verifier.bindFromRequestAndVerify(modelForm)(request, context)) must
                throwA[IllegalStateException]
            checkRecaptchaNotInvoked(mockRequest)
        }

        "throw exception if multiple recaptcha responses" in new WithApplication(validV1Application) {
            val (verifier, mockRequest) = createMocks(OK, None, None, false)
            val request = FakeRequest().withFormUrlEncodedBody(
                    			"field1" -> "aaa",
                    			RecaptchaVerifier.recaptchaChallengeField -> "c",
                    			RecaptchaVerifier.recaptchaResponseField -> "r1",
                    			RecaptchaVerifier.recaptchaResponseField -> "r2"
                    		).withHeaders(CONTENT_TYPE -> MimeTypes.FORM)

            await(verifier.bindFromRequestAndVerify(modelForm)(request, context)) must
                throwA[IllegalStateException]
            checkRecaptchaNotInvoked(mockRequest)
        }

        "return form error if empty recaptcha response" in new WithApplication(validV1Application) {
            val (verifier, mockRequest) = createMocks(OK, None, None, false)
            val request = FakeRequest().withFormUrlEncodedBody(
                    			"field1" -> "aaa",
                    			RecaptchaVerifier.recaptchaChallengeField -> "c",
                    			RecaptchaVerifier.recaptchaResponseField -> ""
                    		).withHeaders(CONTENT_TYPE -> MimeTypes.FORM)

            val result = await(verifier.bindFromRequestAndVerify(modelForm)(request, context))

            result.hasErrors must equalTo(true)
            result.error(RecaptchaVerifier.formErrorKey) must
            		equalTo(Some(FormError(
            		    RecaptchaVerifier.formErrorKey, RecaptchaErrorCode.responseMissing)))
            checkRecaptchaNotInvoked(mockRequest)
        }

        "return form errors if empty recaptcha response and form bind error" in
                new WithApplication(validV1Application) {
            val (verifier, mockRequest) = createMocks(OK, None, None, false)
            val request = FakeRequest().withFormUrlEncodedBody(
                    			"field2" -> "aaa", // not a number
                    			RecaptchaVerifier.recaptchaChallengeField -> "c",
                    			RecaptchaVerifier.recaptchaResponseField -> "" // response missing
                    		).withHeaders(CONTENT_TYPE -> MimeTypes.FORM)

            val result = await(verifier.bindFromRequestAndVerify(modelForm)(request, context))

            result.hasErrors must equalTo(true)
            result.error("field2") must equalTo(Some(FormError("field2", "error.number")))
            result.error(RecaptchaVerifier.formErrorKey) must
            		equalTo(Some(FormError(
            		    RecaptchaVerifier.formErrorKey, RecaptchaErrorCode.responseMissing)))
            checkRecaptchaNotInvoked(mockRequest)
        }

        "return form error if recaptcha error" in new WithApplication(validV1Application) {
            val (verifier, mockRequest) = createMocks(OK,
                    Some("false\nincorrect-captcha-sol", Left(Error(RecaptchaErrorCode.captchaIncorrect))),
                        None, false)
            val request = FakeRequest().withFormUrlEncodedBody(
                    			"field1" -> "aaa",
                    			RecaptchaVerifier.recaptchaChallengeField -> "c",
                    			RecaptchaVerifier.recaptchaResponseField -> "r"
                    		).withHeaders(CONTENT_TYPE -> MimeTypes.FORM)

            val result = await(verifier.bindFromRequestAndVerify(modelForm)(request, context))

            result.hasErrors must equalTo(true)
            result.error(RecaptchaVerifier.formErrorKey) must
            		equalTo(Some(FormError(
            		    RecaptchaVerifier.formErrorKey, RecaptchaErrorCode.captchaIncorrect)))
            checkRecaptchaV1Request(mockRequest, "c", "r", "127.0.0.1", privateKey)
        }

        "return form if recaptcha is successful" in new WithApplication(validV1Application) {
            val (verifier, mockRequest) =
                createMocks(OK, Some("true\ncorrect", Right(Success())), None, false)
            val request = FakeRequest().withFormUrlEncodedBody(
                    			"field1" -> "aaa",
                    			RecaptchaVerifier.recaptchaChallengeField -> "c",
                    			RecaptchaVerifier.recaptchaResponseField -> "r"
                    		).withHeaders(CONTENT_TYPE -> MimeTypes.FORM)

            val result = await(verifier.bindFromRequestAndVerify(modelForm)(request, context))

            result.hasErrors must equalTo(false)
            checkRecaptchaV1Request(mockRequest, "c", "r", "127.0.0.1", privateKey)
        }
    }

    "(v2) RecaptchaVerifier (high level API)" should {

        "throw exception on construction if plugin not enabled" in
                new WithApplication(invalidApplication) {
            createMocks(OK, None, None, false) must throwA[RecaptchaConfigurationException]
        }

        "stop at a binding error and not call recaptcha" in new WithApplication(validV2Application) {
            val (verifier, mockRequest) = createMocks(OK, None, None, false)
            val request = FakeRequest().withFormUrlEncodedBody(
                    			"field2" -> "aaa", // not a number
                    			RecaptchaVerifier.recaptchaV2ResponseField -> "r"
                    		).withHeaders(CONTENT_TYPE -> MimeTypes.FORM)

            val result = await(verifier.bindFromRequestAndVerify(modelForm)(request, context))

            result.hasErrors must equalTo(true)
            result.error("field2") must equalTo(Some(FormError("field2", "error.number")))
            checkRecaptchaNotInvoked(mockRequest)
        }

        "throw exception if no recaptcha response" in new WithApplication(validV2Application) {
            val (verifier, mockRequest) = createMocks(OK, None, None, false)
            val request = FakeRequest().withFormUrlEncodedBody(
                    			"field1" -> "aaa"
                    		).withHeaders(CONTENT_TYPE -> MimeTypes.FORM)

            await(verifier.bindFromRequestAndVerify(modelForm)(request, context)) must
                throwA[IllegalStateException]
            checkRecaptchaNotInvoked(mockRequest)
        }

        "throw exception if multiple recaptcha responses" in new WithApplication(validV2Application) {
            val (verifier, mockRequest) = createMocks(OK, None, None, false)
            val request = FakeRequest().withFormUrlEncodedBody(
                    			"field1" -> "aaa",
                    			RecaptchaVerifier.recaptchaV2ResponseField -> "r1",
                    			RecaptchaVerifier.recaptchaV2ResponseField -> "r2"
                    		).withHeaders(CONTENT_TYPE -> MimeTypes.FORM)

            await(verifier.bindFromRequestAndVerify(modelForm)(request, context)) must
                throwA[IllegalStateException]
            checkRecaptchaNotInvoked(mockRequest)
        }

        "return form error if empty recaptcha response" in new WithApplication(validV2Application) {
            val (verifier, mockRequest) = createMocks(OK, None, None, false)
            val request = FakeRequest().withFormUrlEncodedBody(
                    			"field1" -> "aaa",
                    			RecaptchaVerifier.recaptchaV2ResponseField -> ""
                    		).withHeaders(CONTENT_TYPE -> MimeTypes.FORM)

            val result = await(verifier.bindFromRequestAndVerify(modelForm)(request, context))

            result.hasErrors must equalTo(true)
            result.error(RecaptchaVerifier.formErrorKey) must
            		equalTo(Some(FormError(
            		    RecaptchaVerifier.formErrorKey, RecaptchaErrorCode.responseMissing)))
            checkRecaptchaNotInvoked(mockRequest)
        }

        "return form errors if empty recaptcha response and form bind error" in
                new WithApplication(validV2Application) {
            val (verifier, mockRequest) = createMocks(OK, None, None, false)
            val request = FakeRequest().withFormUrlEncodedBody(
                    			"field2" -> "aaa", // not a number
                    			RecaptchaVerifier.recaptchaV2ResponseField -> "" // response missing
                    		).withHeaders(CONTENT_TYPE -> MimeTypes.FORM)

            val result = await(verifier.bindFromRequestAndVerify(modelForm)(request, context))

            result.hasErrors must equalTo(true)
            result.error("field2") must equalTo(Some(FormError("field2", "error.number")))
            result.error(RecaptchaVerifier.formErrorKey) must
            		equalTo(Some(FormError(
            		    RecaptchaVerifier.formErrorKey, RecaptchaErrorCode.responseMissing)))
            checkRecaptchaNotInvoked(mockRequest)
        }

        "return form error if recaptcha error" in new WithApplication(validV2Application) {
            val (verifier, mockRequest) = createMocks(OK, None,
                    Some(Json.parse("{}"), Left(Error(RecaptchaErrorCode.captchaIncorrect))),
                        false)
            val request = FakeRequest().withFormUrlEncodedBody(
                    			"field1" -> "aaa",
                    			RecaptchaVerifier.recaptchaV2ResponseField -> "r"
                    		).withHeaders(CONTENT_TYPE -> MimeTypes.FORM)

            val result = await(verifier.bindFromRequestAndVerify(modelForm)(request, context))

            result.hasErrors must equalTo(true)
            result.error(RecaptchaVerifier.formErrorKey) must
            		equalTo(Some(FormError(
            		    RecaptchaVerifier.formErrorKey, RecaptchaErrorCode.captchaIncorrect)))
            checkRecaptchaV2Request(mockRequest, "r", "127.0.0.1", privateKey)
        }

        "return form if recaptcha is successful" in new WithApplication(validV2Application) {
            val (verifier, mockRequest) =
                createMocks(OK, None, Some(Json.parse("{}"), Right(Success())), false)
            val request = FakeRequest().withFormUrlEncodedBody(
                    			"field1" -> "aaa",
                    			RecaptchaVerifier.recaptchaV2ResponseField -> "r"
                    		).withHeaders(CONTENT_TYPE -> MimeTypes.FORM)

            val result = await(verifier.bindFromRequestAndVerify(modelForm)(request, context))

            result.hasErrors must equalTo(false)
            checkRecaptchaV2Request(mockRequest, "r", "127.0.0.1", privateKey)
        }
    }

    /**
     * Creates a verifier wired up with mocked dependencies.
     * @param recaptchaResponseCode		The HTTP response code that recaptcha should return
     * @param v1bodyAndParserResult		If set, the v1 response body and parser result to use
     * @param v2bodyAndParserResult		If set, the v2 response body and parser result to use
     * @param futureThrowsError         Whether the web service future should throw an IOException
     * @return The verifier and the web service request
     */
    private def createMocks(recaptchaResponseCode: Int,
            v1bodyAndParserResult: Option[(String, Either[Error, Success])],
            v2bodyAndParserResult: Option[(JsValue, Either[Error, Success])],
                futureThrowsError: Boolean)(implicit application: Application): (RecaptchaVerifier, WSRequest) = {
        val mockWSClient = mock[WSClient]
        val mockRequest = mock[WSRequest]
        val mockResponse = mock[WSResponse]
        val futureResponse = Future {
            if (futureThrowsError) throw new IOException("Oops") else mockResponse
        }
        val mockParser = mock[ResponseParser]

        mockWSClient.url(recaptchaUrls.getVerifyUrl) returns mockRequest
        mockRequest.withRequestTimeout(5.seconds) returns mockRequest

        // I'm sure there's a better way of doing this, but this is the only way I can get the
        // post method call to match. Note in mockito we have to match all the implicit params too
        mockRequest.post(any[Map[String,Seq[String]]])(
            any[Writeable[Map[String,Seq[String]]]]) returns futureResponse

        mockResponse.status returns recaptchaResponseCode

        // mock the API v1 parser input and output
        v1bodyAndParserResult.map { case(body, parserResult) => {
                mockResponse.body returns body
                mockParser.parseV1Response(body) returns parserResult
            }
        }

        // mock the API v2 parser input and output
        v2bodyAndParserResult.map { case(body, parserResult) => {
                mockResponse.json returns body
                mockParser.parseV2Response(body) returns parserResult
            }
        }

        val verifier = new RecaptchaVerifier(mockParser, mockWSClient, recaptchaModuleProv, configuration, recaptchaUrls)

        (verifier, mockRequest)
    }

    /**
     * Checks the API v1 request sent to recaptcha.
     * @param request			The mock request to check
     * @param challenge			The recaptcha challenge
     * @param response			The recaptcha response
     * @param remoteIP			The remote IP
     * @param privateKey		The recaptcha private key
     */
    private def checkRecaptchaV1Request(request: WSRequest, challenge: String,
            response: String, remoteIP: String, privateKey: String): Unit = {
        val captor = ArgumentCaptor.forClass(classOf[Map[String, Seq[String]]])
	    there was one(request).post(captor.capture())(any[Writeable[Map[String,Seq[String]]]])
	    val argument = captor.getValue()
	    argument("challenge") must equalTo(Seq(challenge))
	    argument("response") must equalTo(Seq(response))
	    argument("remoteip") must equalTo(Seq(remoteIP))
	    argument("privatekey") must equalTo(Seq(privateKey))
    }

    /**
     * Checks the API v2 request sent to recaptcha.
     * @param request			The mock request to check
     * @param response			The recaptcha response
     * @param remoteIP			The remote IP
     * @param privateKey		The recaptcha private key
     */
    private def checkRecaptchaV2Request(request: WSRequest, response: String,
            remoteIP: String, privateKey: String): Unit = {
        val captor = ArgumentCaptor.forClass(classOf[Map[String, Seq[String]]])
	    there was one(request).post(captor.capture())(any[Writeable[Map[String,Seq[String]]]])
	    val argument = captor.getValue()
	    argument("response") must equalTo(Seq(response))
	    argument("remoteip") must equalTo(Seq(remoteIP))
	    argument("secret") must equalTo(Seq(privateKey))
    }

    /**
     * Checks that the recaptcha request was never sent.
     * @param request			The mock request to check
     */
    private def checkRecaptchaNotInvoked(request: WSRequest): Unit = {
        there was no(request).post(any[Map[String,Seq[String]]])(
                any[Writeable[Map[String,Seq[String]]]])
    }
}
