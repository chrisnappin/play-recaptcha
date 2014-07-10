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

import play.api.Play.current
import play.api.Logger
import play.api.mvc.{AnyContent, Request}
import play.api.data.Form
import play.api.libs.ws.{WS, WSRequestHolder}

import scala.concurrent.{ExecutionContext, Future}

object RecaptchaVerifier {
    /** The artificial form field key used for captcha errors. */
    val formErrorKey = "com.nappin.play.recaptcha.error"
    
    /** The recaptcha challenge field name. */ 
    val recaptchaChallengeField = "recaptcha_challenge_field"
    
    /** The recaptcha response field name. */ 
    val recaptchaResponseField = "recaptcha_response_field"
}

/**
 * Verifies whether a recaptcha response is valid, by invoking the Google Recaptcha verify web service.
 * 
 * Follows the API documented at <a href="https://developers.google.com/recaptcha/docs/verify">Verify Without Plugins</a>.
 * 
 * Allows constructor injection of dependencies (e.g. for unit testing) with no-arguments constructor for production 
 * use.
 * 
 * @author Chris Nappin
 */
class RecaptchaVerifier(parser: ResponseParser, wsRequest: WSRequestHolder) {

    val logger = Logger(this.getClass())  
    
    /**
     * Constructor for production use.
     */
    def this() = this(new ResponseParser(), WS.url("http://www.google.com/recaptcha/api/verify"))   
    
    /**
     * High level API (using Play forms).
     * 
     * Binds form data from the request, and if valid then verifies the recaptcha response, by invoking the Google 
     * Recaptcha verify web service in a reactive manner. Possible errors include:
     * <ul>
     *   <li>Binding error (form validation, regardless of recaptcha)</li>
     *   <li>Recaptcha response missing (end user didn't enter it)</li>
     *   <li>Recpatcha response incorrect</li>
     *   <li>Error invoking the recaptcha service</li>
     * </ul>
     * 
     * Apart from generic binding error, the recaptcha errors are populated against the artificial form field key 
     * <code>formErrorKey</code>, handled by the recaptcha view template tags.
     * 
     * @param form          The form
     * @param request		Implicit - The web request from the form that included the recaptcha fields
     * @param context       Implicit - The execution context used for futures
     * @return A future that will be the form to use, either populated with an error or success.
     * @throws IllegalStateException Developer errors that shouldn't happen - Plugin not present or not enabled, 
     * No recaptcha challenge, or muliple challenges or responses found
     */
    def bindFromRequestAndVerify[T](form: Form[T])(implicit request: Request[AnyContent], context: ExecutionContext): 
            Future[Form[T]] = {
        checkPluginEnabled()
        
        val boundForm = form.bindFromRequest
        val (challenge, response) = readChallengeAndResponse(request.body.asFormUrlEncoded.get)
        
        if (response.size < 1) {
            // probably an end user error
            logger.debug("User did not enter a captcha response in the form POST submitted")
            
            // return the missing required field, plus any other form bind errors that might have happened
            return Future { boundForm.withError(RecaptchaVerifier.formErrorKey, RecaptchaErrorCode.responseMissing) }
        }
        
        boundForm.fold(
            // form binding failed, so don't call recaptcha
            error => Future { 
                logger.debug("Binding error")
                error 
            },
            
            // form binding succeeded, so verify the captcha response
            success => {
		        verify(challenge, response, request.remoteAddress).map { response => {
	                response.fold(
	                    // captcha incorrect, or a technical error    
	                    error => boundForm.withError(RecaptchaVerifier.formErrorKey, error.code),
	                    
	                    // all success
	                    success => boundForm
	                )
		        }        
		    }
        })
    }
    
    /**
     * Read the challenge and response fields from the POST'ed response.
     * @param params		The POST parameters
     * @return A tuple of challenge and response
     * @throws IllegalStateException If challenge missing, multiple or empty, or response missing or multiple
     */
    private def readChallengeAndResponse(params: Map[String, Seq[String]]): (String, String) = {
        
        logger.debug("POST has " + params.size + " params")
        params.keySet.foreach(param => {
            logger.debug("Param " + param + "=>" + params(param).mkString(","))
        })
        
        if (!params.contains(RecaptchaVerifier.recaptchaChallengeField)) {
            // probably a developer error
            val message = "No recaptcha challenge POSTed, check the form submitted was valid"
            logger.error(message)
            throw new IllegalStateException(message)
        
        } else if (params(RecaptchaVerifier.recaptchaChallengeField).size > 1) {
            // probably a developer error
            val message = "Multiple recaptcha challenge POSTed, check the form submitted was valid"
            logger.error(message)
            throw new IllegalStateException(message)
        
        } else if (params(RecaptchaVerifier.recaptchaChallengeField)(0).size < 1) {
            // probably a developer error
            val message = "Recaptcha challenge was empty, check the form submitted was valid"
            logger.error(message)
            throw new IllegalStateException(message)
        }
        val challenge = params(RecaptchaVerifier.recaptchaChallengeField)(0)
        
        if (!params.contains(RecaptchaVerifier.recaptchaResponseField)) {
            // probably a developer error
            val message = "No recaptcha response POSTed, check the form submitted was valid"
            logger.error(message)
            throw new IllegalStateException(message)
        
        } else if (params(RecaptchaVerifier.recaptchaResponseField).size > 1) {
            // probably a developer error
            val message = "Multiple recaptcha responses POSTed, check the form submitted was valid"
            logger.error(message)
            throw new IllegalStateException(message)
        }
        val response = params(RecaptchaVerifier.recaptchaResponseField)(0)
        
        return (challenge, response)
    }
    
    /**
     * Low level API (independent of Play form and request APIs).
     * 
     * Verifies whether a recaptcha response is valid, by invoking the Google Recaptcha verify web service in a 
     * reactive manner.
     * 
     * @param challenge		The recaptcha challenge
     * @param response		The recaptcha response, to verify
     * @param remoteIp		The IP address of the end user
     * @param context       Implicit - The execution context used for futures
     * @return A future that will be either an Error (with a code) or Success
     * @throws IllegalStateException Developer errors that shouldn't happen - Plugin not present or not enabled
     */
    def verify(challenge: String, response: String, remoteIp: String)(implicit context: ExecutionContext): 
    		Future[Either[Error, Success]] = {
        checkPluginEnabled()
        
        // create the POST payload
        val payload = Map(
            "privatekey" -> Seq(current.configuration.getString(RecaptchaConfiguration.privateKey).get),
	        "remoteip" -> Seq(remoteIp),
	        "challenge" -> Seq(challenge),
	        "response" -> Seq(response)
        )
        
        val requestTimeout = current.configuration.getMilliseconds(RecaptchaConfiguration.requestTimeout)
        							.getOrElse(RecaptchaConfiguration.defaultRequestTimeout)
        
        logger.info(s"Verifying recaptcha ($response) for $remoteIp")
        val futureResponse = wsRequest.withRequestTimeout(requestTimeout.toInt).post(payload)
        
        futureResponse.map { response => {
                if (response.status == play.api.http.Status.OK) {
                    parser.parseResponse(response.body)
                
                } else {
                    logger.error("Error calling recaptcha API, HTTP response " + response.status)
                    Left(Error(RecaptchaErrorCode.recaptchaNotReachable))
                }
            }
        
        } recover {
            case ex: java.io.IOException => {
                logger.error("Unable to call recaptcha API" , ex)
                Left(Error(RecaptchaErrorCode.recaptchaNotReachable))
            }
        }
    }
    
    /**
     * Checks whether the plugin is enabled.
     * @throws IllegalStateException Developer errors that shouldn't happen - plugin is not present, or is not enabled 
     * (e.g. because mandatory configuration is missing)
     */
    private def checkPluginEnabled(): Unit = {
        val plugin = current.plugin[RecaptchaPlugin].getOrElse({
            val message = "Recaptcha Plugin not found"
            logger.error(message)
            throw new IllegalStateException(message)
        })
        
        if (!plugin.enabled) {
            val message = "Recaptcha Plugin is not enabled, please see earlier error log messages as to why"
            logger.error(message)
            throw new IllegalStateException(message)
        }
    }
}

/**
 * Used to hold various recaptcha error codes. Some are defined by Google Recaptcha, some are used solely by this
 * module. Yes, the Google Recpatcha documentation states not to rely on these, so we deliberately keep these to a
 * minimum.
 */
object RecaptchaErrorCode {
    
    /** Defined in Google Recaptcha documentation, returned by Recaptcha itself, means the captcha was incorrect. */
    val captchaIncorrect = "incorrect-captcha-sol" 
        
    /** Defined in Google Recaptcha documentation, used by this module, means recaptcha itself couldn't be reached. */
    val recaptchaNotReachable = "recaptcha-not-reachable"
        
    /** An API error (e.g. invalid format response). */
    val apiError = "recaptcha-api-error"    
        
    /** The recaptcha response was missing from the request (probably an end user error). */     
    val responseMissing = "recaptcha-response-missing"    
        
    /** Error codes that are only for internal use by this module, and shouldn't be passed to the recaptcha API. */    
    val internalErrorCodes = Seq(recaptchaNotReachable, apiError, responseMissing)  
    
    /**
     * Determine whether the specified error code is for internal use only by this module, and shouldn't be passed 
     * to the recaptcha API.
     * @param errorCode		The error code
     * @return <code>true</code> if internal
     */
    def isInternalErrorCode(errorCode: String): Boolean = {
        internalErrorCodes.contains(errorCode)
    }    
}
