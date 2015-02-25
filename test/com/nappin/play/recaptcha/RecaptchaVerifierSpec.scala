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

import org.mockito.ArgumentCaptor
import org.specs2.mock.Mockito
import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith

import play.api.data._
import play.api.data.Forms._
import play.api.http.{ContentTypeOf, MimeTypes, Writeable}
import play.api.libs.json.{Json, JsValue}
import play.api.libs.ws.{WSRequestHolder, WSResponse}
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
    val plugins = Seq("com.nappin.play.recaptcha.RecaptchaPlugin")
        
    val validV1Application = new FakeApplication(
            additionalPlugins = plugins,
            additionalConfiguration = Map(
                RecaptchaConfiguration.privateKey -> "private-key",
                RecaptchaConfiguration.publicKey -> "public-key",
                RecaptchaConfiguration.apiVersion -> "1",
                RecaptchaConfiguration.requestTimeout -> "5 seconds"))
    
    val validV2Application = new FakeApplication(
            additionalPlugins = plugins,
            additionalConfiguration = Map(
                RecaptchaConfiguration.privateKey -> "private-key",
                RecaptchaConfiguration.publicKey -> "public-key",
                RecaptchaConfiguration.apiVersion -> "2",
                RecaptchaConfiguration.requestTimeout -> "5 seconds"))
    
    /** Has mandatory configuration missing. */    
    val invalidApplication = new FakeApplication(additionalPlugins = plugins)
    
    "(v1) RecaptchaVerifier (low level API)" should {
        
        "throw exception if plugin not enabled" in new WithApplication(invalidApplication) {
            val (verifier, mockWS) = createMocks(OK, None, None, false)
            
            await(verifier.verifyV1("aaa", "bbb", "ccc")) must throwA[IllegalStateException]
            
            checkRecaptchaNotInvoked(mockWS)
        }
        
        "handle a valid response as a success" in new WithApplication(validV1Application) {
            val (verifier, mockWS) = 
                createMocks(OK, Some("true\nSuccess", Right(Success())), None, false)
            
            verifier.verifyV1("aaa", "bbb", "ccc") must equalTo(Right(Success())).await
            
            checkRecaptchaV1Request(mockWS, "aaa", "bbb", "ccc", privateKey)
        }
        
        "handle a parser error as an error" in new WithApplication(validV1Application) {
            val (verifier, mockWS) = 
                createMocks(OK, Some("invaid response", Left(Error(RecaptchaErrorCode.apiError))), 
                        None, false)
            
            verifier.verifyV1("aaa", "bbb", "ccc") must 
                equalTo(Left(Error(RecaptchaErrorCode.apiError))).await
            
            checkRecaptchaV1Request(mockWS, "aaa", "bbb", "ccc", privateKey)
        }
        
        "handle a 404 response as an error" in new WithApplication(validV1Application) {
            val (verifier, mockWS) = createMocks(NOT_FOUND, None, None, false)
            
            verifier.verifyV1("aaa", "bbb", "ccc") must 
                equalTo(Left(Error(RecaptchaErrorCode.recaptchaNotReachable))).await
            
            checkRecaptchaV1Request(mockWS, "aaa", "bbb", "ccc", privateKey)
        }
        
        "handle an IOException as an error" in new WithApplication(validV1Application) {
            val (verifier, mockWS) = createMocks(OK, None, None, true)
            
            verifier.verifyV1("aaa", "bbb", "ccc") must 
                equalTo(Left(Error(RecaptchaErrorCode.recaptchaNotReachable))).await
            
            checkRecaptchaV1Request(mockWS, "aaa", "bbb", "ccc", privateKey)
        } 
    }
    
    "(v2) RecaptchaVerifier (low level API)" should {
        
        "throw exception if plugin not enabled" in new WithApplication(invalidApplication) {
            val (verifier, mockWS) = createMocks(OK, None, None, false)
            
            await(verifier.verifyV2("aaa", "bbb")) must throwA[IllegalStateException]
            
            checkRecaptchaNotInvoked(mockWS)
        }
        
        "handle a valid response as a success" in new WithApplication(validV2Application) {
            val (verifier, mockWS) = 
                createMocks(OK, None, Some(Json.parse("{\"success\":true}"), Right(Success())), false)
            
            verifier.verifyV2("aaa", "bbb") must equalTo(Right(Success())).await
            
            checkRecaptchaV2Request(mockWS, "aaa", "bbb", privateKey)
        }
        
        "handle a parser error as an error" in new WithApplication(validV2Application) {
            val (verifier, mockWS) = 
                createMocks(OK, None, Some(Json.parse("{}"), Left(Error(RecaptchaErrorCode.apiError))), false)
            
            verifier.verifyV2("aaa", "bbb") must 
                equalTo(Left(Error(RecaptchaErrorCode.apiError))).await
            
            checkRecaptchaV2Request(mockWS, "aaa", "bbb", privateKey)
        }
        
        "handle a 404 response as an error" in new WithApplication(validV2Application) {
            val (verifier, mockWS) = createMocks(NOT_FOUND, None, None, false)
            
            verifier.verifyV2("aaa", "bbb") must 
                equalTo(Left(Error(RecaptchaErrorCode.recaptchaNotReachable))).await
            
            checkRecaptchaV2Request(mockWS, "aaa", "bbb", privateKey)
        }
        
        "handle an IOException as an error" in new WithApplication(validV2Application) {
            val (verifier, mockWS) = createMocks(OK, None, None, true)
            
            verifier.verifyV2("aaa", "bbb") must 
                equalTo(Left(Error(RecaptchaErrorCode.recaptchaNotReachable))).await
            
            checkRecaptchaV2Request(mockWS, "aaa", "bbb", privateKey)
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
        
        "throw exception if plugin not enabled" in new WithApplication(invalidApplication) {
            val (verifier, mockWS) = createMocks(OK, None, None, false)
            val request = FakeRequest().withFormUrlEncodedBody("field1" -> "aaa")
            				.withHeaders(CONTENT_TYPE -> MimeTypes.FORM)
            
            await(verifier.bindFromRequestAndVerify(modelForm)(request, context)) must 
                throwA[IllegalStateException]
            checkRecaptchaNotInvoked(mockWS)
        }
        
        "stop at a binding error and not call recaptcha" in new WithApplication(validV1Application) {
            val (verifier, mockWS) = createMocks(OK, None, None, false)
            val request = FakeRequest().withFormUrlEncodedBody(
                    			"field2" -> "aaa", // not a number
                    			RecaptchaVerifier.recaptchaChallengeField -> "c",
                    			RecaptchaVerifier.recaptchaResponseField -> "r"
                    		).withHeaders(CONTENT_TYPE -> MimeTypes.FORM)
            
            val result = await(verifier.bindFromRequestAndVerify(modelForm)(request, context))
            
            result.hasErrors must equalTo(true)
            result.error("field2") must equalTo(Some(FormError("field2", "error.number")))
            checkRecaptchaNotInvoked(mockWS)
        }
        
        "throw exception if no recaptcha challenge" in new WithApplication(validV1Application) {
            val (verifier, mockWS) = createMocks(OK, None, None, false)
            val request = FakeRequest().withFormUrlEncodedBody("field1" -> "aaa")
            				.withHeaders(CONTENT_TYPE -> MimeTypes.FORM)
            
            await(verifier.bindFromRequestAndVerify(modelForm)(request, context)) must 
                throwA[IllegalStateException]
            checkRecaptchaNotInvoked(mockWS)
        }
        
        "throw exception if empty recaptcha challenge" in new WithApplication(validV1Application) {
            val (verifier, mockWS) = createMocks(OK, None, None, false)
            val request = FakeRequest().withFormUrlEncodedBody(
                    			"field1" -> "aaa", 
                    			RecaptchaVerifier.recaptchaChallengeField -> ""
                    		).withHeaders(CONTENT_TYPE -> MimeTypes.FORM)
            
            await(verifier.bindFromRequestAndVerify(modelForm)(request, context)) must 
                throwA[IllegalStateException]
            checkRecaptchaNotInvoked(mockWS)
        }
        
        "throw exception if multiple recaptcha challenges" in new WithApplication(validV1Application) {
            val (verifier, mockWS) = createMocks(OK, None, None, false)
            val request = FakeRequest().withFormUrlEncodedBody(
                    			"field1" -> "aaa", 
                    			RecaptchaVerifier.recaptchaChallengeField -> "c1",
                    			RecaptchaVerifier.recaptchaChallengeField -> "c2"
                    		).withHeaders(CONTENT_TYPE -> MimeTypes.FORM)
            
            await(verifier.bindFromRequestAndVerify(modelForm)(request, context)) must 
                throwA[IllegalStateException]
            checkRecaptchaNotInvoked(mockWS)
        }
        
        "throw exception if no recaptcha response" in new WithApplication(validV1Application) {
            val (verifier, mockWS) = createMocks(OK, None, None, false)
            val request = FakeRequest().withFormUrlEncodedBody(
                    			"field1" -> "aaa", 
                    			RecaptchaVerifier.recaptchaChallengeField -> "c"
                    		).withHeaders(CONTENT_TYPE -> MimeTypes.FORM)
            
            await(verifier.bindFromRequestAndVerify(modelForm)(request, context)) must 
                throwA[IllegalStateException]
            checkRecaptchaNotInvoked(mockWS)
        }
        
        "throw exception if multiple recaptcha responses" in new WithApplication(validV1Application) {
            val (verifier, mockWS) = createMocks(OK, None, None, false)
            val request = FakeRequest().withFormUrlEncodedBody(
                    			"field1" -> "aaa", 
                    			RecaptchaVerifier.recaptchaChallengeField -> "c",
                    			RecaptchaVerifier.recaptchaResponseField -> "r1",
                    			RecaptchaVerifier.recaptchaResponseField -> "r2"
                    		).withHeaders(CONTENT_TYPE -> MimeTypes.FORM)
            
            await(verifier.bindFromRequestAndVerify(modelForm)(request, context)) must 
                throwA[IllegalStateException]
            checkRecaptchaNotInvoked(mockWS)
        }
        
        "return form error if empty recaptcha response" in new WithApplication(validV1Application) {
            val (verifier, mockWS) = createMocks(OK, None, None, false)
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
            checkRecaptchaNotInvoked(mockWS)
        }
        
        "return form errors if empty recaptcha response and form bind error" in 
                new WithApplication(validV1Application) {
            val (verifier, mockWS) = createMocks(OK, None, None, false)
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
            checkRecaptchaNotInvoked(mockWS)
        }
        
        "return form error if recaptcha error" in new WithApplication(validV1Application) {
            val (verifier, mockWS) = createMocks(OK, 
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
            checkRecaptchaV1Request(mockWS, "c", "r", "127.0.0.1", privateKey)
        }
        
        "return form if recaptcha is successful" in new WithApplication(validV1Application) {
            val (verifier, mockWS) = 
                createMocks(OK, Some("true\ncorrect", Right(Success())), None, false)
            val request = FakeRequest().withFormUrlEncodedBody(
                    			"field1" -> "aaa", 
                    			RecaptchaVerifier.recaptchaChallengeField -> "c",
                    			RecaptchaVerifier.recaptchaResponseField -> "r"
                    		).withHeaders(CONTENT_TYPE -> MimeTypes.FORM)
            
            val result = await(verifier.bindFromRequestAndVerify(modelForm)(request, context))
            
            result.hasErrors must equalTo(false)
            checkRecaptchaV1Request(mockWS, "c", "r", "127.0.0.1", privateKey)
        }
    }
    
    "(v2) RecaptchaVerifier (high level API)" should {
        
        "throw exception if plugin not enabled" in new WithApplication(invalidApplication) {
            val (verifier, mockWS) = createMocks(OK, None, None, false)
            val request = FakeRequest().withFormUrlEncodedBody("field1" -> "aaa")
            				.withHeaders(CONTENT_TYPE -> MimeTypes.FORM)
            
            await(verifier.bindFromRequestAndVerify(modelForm)(request, context)) must 
                throwA[IllegalStateException]
            checkRecaptchaNotInvoked(mockWS)
        }
        
        "stop at a binding error and not call recaptcha" in new WithApplication(validV2Application) {
            val (verifier, mockWS) = createMocks(OK, None, None, false)
            val request = FakeRequest().withFormUrlEncodedBody(
                    			"field2" -> "aaa", // not a number
                    			RecaptchaVerifier.recaptchaV2ResponseField -> "r"
                    		).withHeaders(CONTENT_TYPE -> MimeTypes.FORM)
            
            val result = await(verifier.bindFromRequestAndVerify(modelForm)(request, context))
            
            result.hasErrors must equalTo(true)
            result.error("field2") must equalTo(Some(FormError("field2", "error.number")))
            checkRecaptchaNotInvoked(mockWS)
        }
        
        "throw exception if no recaptcha response" in new WithApplication(validV2Application) {
            val (verifier, mockWS) = createMocks(OK, None, None, false)
            val request = FakeRequest().withFormUrlEncodedBody(
                    			"field1" -> "aaa"
                    		).withHeaders(CONTENT_TYPE -> MimeTypes.FORM)
            
            await(verifier.bindFromRequestAndVerify(modelForm)(request, context)) must 
                throwA[IllegalStateException]
            checkRecaptchaNotInvoked(mockWS)
        }
        
        "throw exception if multiple recaptcha responses" in new WithApplication(validV2Application) {
            val (verifier, mockWS) = createMocks(OK, None, None, false)
            val request = FakeRequest().withFormUrlEncodedBody(
                    			"field1" -> "aaa", 
                    			RecaptchaVerifier.recaptchaV2ResponseField -> "r1",
                    			RecaptchaVerifier.recaptchaV2ResponseField -> "r2"
                    		).withHeaders(CONTENT_TYPE -> MimeTypes.FORM)
            
            await(verifier.bindFromRequestAndVerify(modelForm)(request, context)) must 
                throwA[IllegalStateException]
            checkRecaptchaNotInvoked(mockWS)
        }
        
        "return form error if empty recaptcha response" in new WithApplication(validV2Application) {
            val (verifier, mockWS) = createMocks(OK, None, None, false)
            val request = FakeRequest().withFormUrlEncodedBody(
                    			"field1" -> "aaa", 
                    			RecaptchaVerifier.recaptchaV2ResponseField -> ""
                    		).withHeaders(CONTENT_TYPE -> MimeTypes.FORM)
            
            val result = await(verifier.bindFromRequestAndVerify(modelForm)(request, context))
            
            result.hasErrors must equalTo(true)
            result.error(RecaptchaVerifier.formErrorKey) must 
            		equalTo(Some(FormError(
            		    RecaptchaVerifier.formErrorKey, RecaptchaErrorCode.responseMissing)))
            checkRecaptchaNotInvoked(mockWS)
        }
        
        "return form errors if empty recaptcha response and form bind error" in 
                new WithApplication(validV2Application) {
            val (verifier, mockWS) = createMocks(OK, None, None, false)
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
            checkRecaptchaNotInvoked(mockWS)
        }
        
        "return form error if recaptcha error" in new WithApplication(validV2Application) {
            val (verifier, mockWS) = createMocks(OK, None,
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
            checkRecaptchaV2Request(mockWS, "r", "127.0.0.1", privateKey)
        }
        
        "return form if recaptcha is successful" in new WithApplication(validV2Application) {
            val (verifier, mockWS) = 
                createMocks(OK, None, Some(Json.parse("{}"), Right(Success())), false)
            val request = FakeRequest().withFormUrlEncodedBody(
                    			"field1" -> "aaa", 
                    			RecaptchaVerifier.recaptchaV2ResponseField -> "r"
                    		).withHeaders(CONTENT_TYPE -> MimeTypes.FORM)
            
            val result = await(verifier.bindFromRequestAndVerify(modelForm)(request, context))
            
            result.hasErrors must equalTo(false)
            checkRecaptchaV2Request(mockWS, "r", "127.0.0.1", privateKey)
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
                futureThrowsError: Boolean): (RecaptchaVerifier, WSRequestHolder) = {
        val mockWS = mock[WSRequestHolder]
        val mockResponse = mock[WSResponse]
        val futureResponse = Future { 
            if (futureThrowsError) throw new IOException("Oops") else mockResponse
        }
        val mockParser = mock[ResponseParser]
        
        mockWS.withRequestTimeout(5.seconds.toMillis.toInt) returns mockWS
        
        // I'm sure there's a better way of doing this, but this is the only way I can get the 
        // post method call to match. Note in mockito we have to match all the implicit params too
        mockWS.post(any[Map[String,Seq[String]]])(any[Writeable[Map[String,Seq[String]]]], 
                any[ContentTypeOf[Map[String,Seq[String]]]]) returns futureResponse
        
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
        
        val verifier = new RecaptchaVerifier(mockParser, mockWS)
        
        return (verifier, mockWS)
    }
    
    /**
     * Checks the API v1 request sent to recaptcha.
     * @param request			The mock request to check
     * @param challenge			The recaptcha challenge
     * @param response			The recaptcha response
     * @param remoteIP			The remote IP
     * @param privateKey		The recaptcha private key
     */
    private def checkRecaptchaV1Request(request: WSRequestHolder, challenge: String, 
            response: String, remoteIP: String, privateKey: String): Unit = {
        val captor = ArgumentCaptor.forClass(classOf[Map[String, Seq[String]]])
	    there was one(request).post(captor.capture())(any[Writeable[Map[String,Seq[String]]]], 
	            any[ContentTypeOf[Map[String,Seq[String]]]])
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
    private def checkRecaptchaV2Request(request: WSRequestHolder, response: String, 
            remoteIP: String, privateKey: String): Unit = {
        val captor = ArgumentCaptor.forClass(classOf[Map[String, Seq[String]]])
	    there was one(request).post(captor.capture())(any[Writeable[Map[String,Seq[String]]]], 
	            any[ContentTypeOf[Map[String,Seq[String]]]])
	    val argument = captor.getValue()
	    argument("response") must equalTo(Seq(response))
	    argument("remoteip") must equalTo(Seq(remoteIP))
	    argument("secret") must equalTo(Seq(privateKey))
    }
    
    /**
     * Checks that the recaptcha request was never sent.
     * @param request			The mock request to check
     */
    private def checkRecaptchaNotInvoked(request: WSRequestHolder): Unit = {
        there was no(request).post(any[Map[String,Seq[String]]])(
                any[Writeable[Map[String,Seq[String]]]], 
                    any[ContentTypeOf[Map[String,Seq[String]]]])
    }
}