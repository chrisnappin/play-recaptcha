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
import play.api.libs.json.Json

/**
 * Tests the <code>ResponseParser</code> class.
 *
 * @author Chris Nappin
 */
@RunWith(classOf[JUnitRunner])
class ResponseParserSpec extends Specification {

    /** The class under test. */
    val parser = new ResponseParser()

    /** Expected result for an API error. */
    val apiError = Left(Error(RecaptchaErrorCode.apiError))

    /** Expected successful result. */
    val successResult = Right(Success())

    "ResponseParser (v1)" should {

        "reject an empty response" in {
            parser.parseV1Response("") must equalTo(apiError)
        }

        "accept a single line success response" in {
            parser.parseV1Response("true") must equalTo(successResult)
        }

        "accept a multiple line success response (no error code)" in {
            parser.parseV1Response("true\n") must equalTo(successResult)
        }

        "accept a multiple line success response (with error code)" in {
            // This is what the API typically returns - error code gets ignored
            parser.parseV1Response("true\nsuccess") must equalTo(successResult)
        }

        "accept a multiple line success response (with multiple lines)" in {
            // API mentions new lines might be added in the future,
            // so for now we simply ignore them
            parser.parseV1Response("true\nwibble\nfurther-data") must equalTo(successResult)
        }

        "reject a single line failure response" in {
            parser.parseV1Response("false") must equalTo(apiError)
        }

        "reject a failure response with no error code" in {
            parser.parseV1Response("false\n") must equalTo(apiError)
        }

        "accept a failure response with error code" in {
            parser.parseV1Response("false\ntest-error") must equalTo(Left(Error("test-error")))
        }

        "accept a failure response with error code (and further lines)" in {
            // API mentions new lines might be added in the future,
            // so for now we simply ignore them
            parser.parseV1Response("false\ntest-error\nfurther-data") must
                equalTo(Left(Error("test-error")))
        }

        "reject an invalid response" in {
            // not true or false
            parser.parseV1Response("wibble") must equalTo(apiError)
        }
    }

    "ResponseParser (v2)" should {

        "reject an empty response" in {
            parser.parseV2Response(Json.parse("{}")) must equalTo(apiError)
        }

        "reject a null response" in {
            parser.parseV2Response(Json.parse("{ \"success\": null }")) must equalTo(apiError)
        }

        "reject a invalid success (wrong type) response" in {
            parser.parseV2Response(Json.parse("{ \"success\": 123 }")) must equalTo(apiError)
        }

        "accept a single line valid response" in {
            parser.parseV2Response(Json.parse("{ \"success\": true }")) must
                equalTo(successResult)
        }

        "accept a multi line valid response" in {
            parser.parseV2Response(Json.parse("{ \n\"success\": true\n }")) must
                equalTo(successResult)
        }

        "accept a failure response without error code" in {
            parser.parseV2Response(Json.parse("{ \"success\": false }")) must
                equalTo(Left(Error("")))
        }

        "accept a failure response with error code" in {
            parser.parseV2Response(Json.parse("""
{
    "success": false,
    "error-codes": ["abc"]
}"""
                )) must equalTo(Left(Error("abc")))
        }

                "accept a failure response with multiple error codes" in {
            parser.parseV2Response(Json.parse("""
{
    "success": false,
    "error-codes": ["aa", "bb", "cc"]
}"""
                )) must equalTo(Left(Error("aa")))
        }
    }
}
