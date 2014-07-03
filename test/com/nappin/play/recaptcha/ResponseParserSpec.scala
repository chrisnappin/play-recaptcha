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
    
    "ResponseParser" should {
        
        "reject an empty response" in {
            parser.parseResponse("") must equalTo(apiError)
        }
        
        "accept a single line success response" in {
            parser.parseResponse("true") must equalTo(successResult)
        }
        
        "accept a multiple line success response (no error code)" in {
            parser.parseResponse("true\n") must equalTo(successResult)
        }
        
        "accept a multiple line success response (with error code)" in {
            // This is what the API typically returns - error code gets ignored
            parser.parseResponse("true\nsuccess") must equalTo(successResult)
        }
        
        "accept a multiple line success response (with multiple lines)" in {
            // API mentions new lines might be added in the future,
            // so for now we simply ignore them
            parser.parseResponse("true\nwibble\nfurther-data") must equalTo(successResult)
        }
        
        "reject a single line failure response" in {
            parser.parseResponse("false") must equalTo(apiError)
        }
        
        "reject a failure response with no error code" in {
            parser.parseResponse("false\n") must equalTo(apiError)
        }
        
        "accept a failure response with error code" in {
            parser.parseResponse("false\ntest-error") must equalTo(Left(Error("test-error")))
        }
        
        "accept a failure response with error code (and further lines)" in {
            // API mentions new lines might be added in the future,
            // so for now we simply ignore them
            parser.parseResponse("false\ntest-error\nfurther-data") must equalTo(Left(Error("test-error")))
        }
        
        "reject an invalid response" in {
            // not true or false
            parser.parseResponse("wibble") must equalTo(apiError)
        }
    }
}