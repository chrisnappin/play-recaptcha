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

import play.api.Logger
import play.api.libs.json.{JsResultException, JsValue}
import javax.inject.{Inject, Singleton}

/**
 * Used to parse verify API responses.
 *
 * Follows the JSON Google reCAPTCHA API, but if it encounters any API errors (e.g. invalid responses) then it returns
 * an error with an artificial error code corresponding to <code>apiError</code>.
 *
 * @author chrisnappin
 */
@Singleton
class ResponseParser @Inject() (){

    val logger = Logger(this.getClass)

    /**
     * Parses a verify API V2 response, which is a very simple JSON object.
     * @param response		The JSON response
     * @return Either an Error (with a populated error code) or a Success
     */
    def parseV2Response(response: JsValue): Either[Error, Success] = {
        try {
            // success boolean flag is mandatory
            val success = (response \ "success").as[Boolean]
	        if (success) Right(Success())
	        else {
	            // error codes are optional
	            val errorCodes = (response \ "error-codes").asOpt[Seq[String]]
	            if (errorCodes.isDefined) {
	                // use the first error code, ignore the rest (if any)
	                logger.info(s"Response was: error => $errorCodes")
	                Left(Error(errorCodes.get.head))

	            } else {
		            // no specific error code supplied
			        logger.info(s"Response was: error")
	                Left(Error(""))
	            }
	        }
        } catch {
            case ex: JsResultException =>
                // anything else doesn't meet the API definition
		        logger.error("Invalid response: " + response)
		        Left(Error(RecaptchaErrorCode.apiError))
        }
    }
}

/** Signifies the recaptcha response was valid. */
case class Success()

/** Signifies a recaptcha error, such as captcha response was incorrect or a technical error of some kind. */
case class Error(code: String)
