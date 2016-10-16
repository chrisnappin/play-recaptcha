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

import java.io.IOException

import org.mockito.ArgumentCaptor
import org.specs2.mock.Mockito
import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith

import play.api.Configuration
import play.api.data._
import play.api.data.Forms._
import play.api.http.{MimeTypes, Writeable}
import play.api.libs.json.{Json, JsValue}
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import play.api.test.{FakeRequest, PlaySpecification, WithApplication}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Left, Right}
import RecaptchaSettings._

/**
 * Tests the <code>RecaptchaVerifier</code> class.
 *
 * @author chrisnappin
 */
@RunWith(classOf[JUnitRunner])
class RecaptchaVerifierSpec extends PlaySpecification with Mockito {

    val privateKey = "private-key"

    val validV2Settings: Map[String, Any] =  Map(
              PrivateKeyConfigProp -> privateKey,
              PublicKeyConfigProp -> "public-key",
              RequestTimeoutConfigProp -> "5 seconds")

    "(v2) RecaptchaVerifier (low level API)" should {

        "handle a valid response as a success" in {
            val (verifier, mockRequest) =
                createMocks(validV2Settings, OK, Some(Json.parse("{\"success\":true}"), Right(Success())), false)

            await(verifier.verifyV2("aaa", "bbb")) must equalTo(Right(Success()))

            checkRecaptchaV2Request(mockRequest, "aaa", "bbb", privateKey)
        }

        "handle a parser error as an error" in {
            val (verifier, mockRequest) =
                createMocks(validV2Settings, OK, Some(Json.parse("{}"), Left(Error(RecaptchaErrorCode.apiError))), false)

            await(verifier.verifyV2("aaa", "bbb")) must
                equalTo(Left(Error(RecaptchaErrorCode.apiError)))

            checkRecaptchaV2Request(mockRequest, "aaa", "bbb", privateKey)
        }

        "handle a 404 response as an error" in {
            val (verifier, mockRequest) = createMocks(validV2Settings, NOT_FOUND, None, false)

            await(verifier.verifyV2("aaa", "bbb")) must
                equalTo(Left(Error(RecaptchaErrorCode.recaptchaNotReachable)))

            checkRecaptchaV2Request(mockRequest, "aaa", "bbb", privateKey)
        }

        "handle an IOException as an error" in {
            val (verifier, mockRequest) = createMocks(validV2Settings, OK, None, true)

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

    "RecaptchaVerifier (high level API)" should {

        "stop at a binding error and not call recaptcha" in {
            val (verifier, mockRequest) = createMocks(validV2Settings, OK, None, false)
            val request = FakeRequest().withFormUrlEncodedBody(
                    			"field2" -> "aaa", // not a number
                    			RecaptchaVerifier.recaptchaV2ResponseField -> "r"
                    		).withHeaders(CONTENT_TYPE -> MimeTypes.FORM)

            val result = await(verifier.bindFromRequestAndVerify(modelForm)(request, context))

            result.hasErrors must equalTo(true)
            result.error("field2") must equalTo(Some(FormError("field2", "error.number")))
            checkRecaptchaNotInvoked(mockRequest)
        }

        "throw exception if no recaptcha response" in {
            val (verifier, mockRequest) = createMocks(validV2Settings, OK, None, false)
            val request = FakeRequest().withFormUrlEncodedBody(
                    			"field1" -> "aaa"
                    		).withHeaders(CONTENT_TYPE -> MimeTypes.FORM)

            await(verifier.bindFromRequestAndVerify(modelForm)(request, context)) must
                throwA[IllegalStateException]
            checkRecaptchaNotInvoked(mockRequest)
        }

        "throw exception if multiple recaptcha responses" in {
            val (verifier, mockRequest) = createMocks(validV2Settings, OK, None, false)
            val request = FakeRequest().withFormUrlEncodedBody(
                    			"field1" -> "aaa",
                    			RecaptchaVerifier.recaptchaV2ResponseField -> "r1",
                    			RecaptchaVerifier.recaptchaV2ResponseField -> "r2"
                    		).withHeaders(CONTENT_TYPE -> MimeTypes.FORM)

            await(verifier.bindFromRequestAndVerify(modelForm)(request, context)) must
                throwA[IllegalStateException]
            checkRecaptchaNotInvoked(mockRequest)
        }

        "return form error if empty recaptcha response" in {
            val (verifier, mockRequest) = createMocks(validV2Settings, OK, None, false)
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
                {
            val (verifier, mockRequest) = createMocks(validV2Settings, OK, None, false)
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

        "return form error if recaptcha error" in {
            val (verifier, mockRequest) = createMocks(validV2Settings, OK,
                    Some(Json.parse("{}"), Left(Error(RecaptchaErrorCode.captchaIncorrect))), false)
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

        "return form if recaptcha is successful" in {
            val (verifier, mockRequest) =
                createMocks(validV2Settings, OK, Some(Json.parse("{}"), Right(Success())), false)
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
      *
      * @param configProps App config from which settings will be extracted
      * @param recaptchaResponseCode		The HTTP response code that recaptcha should return
     * @param v2bodyAndParserResult		If set, the v2 response body and parser result to use
     * @param futureThrowsError         Whether the web service future should throw an IOException
     * @return The verifier and the web service request
     */
    private def createMocks(configProps: Map[String, Any], recaptchaResponseCode: Int,
            v2bodyAndParserResult: Option[(JsValue, Either[Error, Success])],
                futureThrowsError: Boolean): (RecaptchaVerifier, WSRequest) = {
      val conf = Configuration.from(configProps)

      val settings = new RecaptchaSettings(conf)

        val mockWSClient = mock[WSClient]
        val mockRequest = mock[WSRequest]
        val mockResponse = mock[WSResponse]
        val futureResponse = Future {
            if (futureThrowsError) throw new IOException("Oops") else mockResponse
        }
        val mockParser = mock[ResponseParser]

        mockWSClient.url(settings.verifyUrl) returns mockRequest
        mockRequest.withRequestTimeout(5.seconds) returns mockRequest

        // I'm sure there's a better way of doing this, but this is the only way I can get the
        // post method call to match. Note in mockito we have to match all the implicit params too
        mockRequest.post(any[Map[String,Seq[String]]])(
            any[Writeable[Map[String,Seq[String]]]]) returns futureResponse

        mockResponse.status returns recaptchaResponseCode

        // mock the API v2 parser input and output
        v2bodyAndParserResult.map { case(body, parserResult) => {
                mockResponse.json returns body
                mockParser.parseV2Response(body) returns parserResult
            }
        }

        val verifier = new RecaptchaVerifier(settings, mockParser, mockWSClient)

        (verifier, mockRequest)
    }

    /**
     * Checks the API v2 request sent to recaptcha.
     *
     * @param request			The mock request to check
     * @param response			The recaptcha response
     * @param remoteIP			The remote IP
     * @param privateKey		The recaptcha private key
     */
    private def checkRecaptchaV2Request(request: WSRequest, response: String, remoteIP: String, privateKey: String) = {
        val captor = ArgumentCaptor.forClass(classOf[Map[String, Seq[String]]])
	    there was one(request).post(captor.capture())(any[Writeable[Map[String,Seq[String]]]])
	    val argument = captor.getValue()
	    argument("response") must equalTo(Seq(response))
	    argument("remoteip") must equalTo(Seq(remoteIP))
	    argument("secret") must equalTo(Seq(privateKey))
    }

    /**
     * Checks that the recaptcha request was never sent.
      *
      * @param request			The mock request to check
     */
    private def checkRecaptchaNotInvoked(request: WSRequest) = {
        there was no(request).post(any[Map[String,Seq[String]]])( any[Writeable[Map[String,Seq[String]]]])
    }
}
