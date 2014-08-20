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
package views.recaptcha

import com.nappin.play.recaptcha.{RecaptchaConfiguration, RecaptchaErrorCode, RecaptchaVerifier}

import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith

import play.api.data._
import play.api.data.Forms._
import play.api.i18n.Messages
import play.api.test.{FakeApplication, FakeRequest, PlaySpecification, WithApplication}

/**
 * Tests the <code>recaptchaField</code> view template.
 * 
 * @author Chris Nappin
 */
@RunWith(classOf[JUnitRunner])
class RecaptchaFieldSpec extends PlaySpecification {

    val scriptApi = "http://www.google.com/recaptcha/api/challenge"
    val noScriptApi = "http://www.google.com/recaptcha/api/noscript"
    val validApplication = 
        new FakeApplication(path = new java.io.File("test-conf/"), 
            additionalConfiguration = Map(
                "application.langs" -> "en,fr",
                RecaptchaConfiguration.privateKey -> "private-key",
                RecaptchaConfiguration.publicKey -> "public-key"))
    
    // used to bind with
    case class Model(field1: String, field2: Option[Int])
    
    val modelForm = Form(mapping(
            "field1" -> nonEmptyText,
            "field2" -> optional(number)
        )(Model.apply)(Model.unapply))
            
    // browser prefers french then english
    implicit val request = FakeRequest().withHeaders(("Accept-Language", "fr; q=1.0, en; q=0.5"))    
    implicit val lang = request.acceptLanguages(0)    
    
    "recaptchaField" should {
            
        "render field without errors, is required default" in new WithApplication(validApplication) {
            val html = contentAsString(views.html.recaptcha.recaptchaField(
                    form = modelForm, fieldName = "myCaptcha")(request, request.acceptLanguages(0)))
            
            // no error passed to recaptcha
            html must contain(s"$scriptApi?k=public-key")
            html must contain(s"$noScriptApi?k=public-key")
            
            // no error shown to end user
            html must not contain("<dd class=\"error\">")
            
            // constraint.required (in french) shown to end user
            html must contain("<dd class=\"info\">Fr-Constraint-Required</dd>")
        }
        
        "render field without errors, is required true" in new WithApplication(validApplication) {
            val html = contentAsString(views.html.recaptcha.recaptchaField(
                    form = modelForm, fieldName = "myCaptcha", isRequired = true))
            
            // no error passed to recaptcha
            html must contain(s"$scriptApi?k=public-key")
            html must contain(s"$noScriptApi?k=public-key")
            
            // no error shown to end user
            html must not contain("<dd class=\"error\">")
            
            // constraint.required (in french) shown to end user
            html must contain("<dd class=\"info\">Fr-Constraint-Required</dd>")
        }
        
        "render field without errors, is required false" in new WithApplication(validApplication) {
            val html = contentAsString(views.html.recaptcha.recaptchaField(
                    form = modelForm, fieldName = "myCaptcha", isRequired = false))
            
            // no error passed to recaptcha
            html must contain(s"$scriptApi?k=public-key")
            html must contain(s"$noScriptApi?k=public-key")
            
            // no error shown to end user
            html must not contain("<dd class=\"error\">")
            
            // constraint.required (in french) not shown to end user
            html must not contain("<dd class=\"info\">Fr-Constraint-Required</dd>")
        }
        
        "treat unknown error as external, not shown to end user" in new WithApplication(validApplication) {
            val html = contentAsString(views.html.recaptcha.recaptchaField(
                    form = modelForm.withError(RecaptchaVerifier.formErrorKey, "my-error-key"), 
                    	fieldName = "myCaptcha"))
            
            // error passed to recaptcha
            html must contain(s"$scriptApi?k=public-key&error=my-error-key")
            html must contain(s"$noScriptApi?k=public-key&error=my-error-key")
            
            // no error shown to end user
            html must not contain("<dd class=\"error\">")
        }
        
        "treat responseMissing as internal, showing error.required" in new WithApplication(validApplication) {
            val html = contentAsString(views.html.recaptcha.recaptchaField(
                    form = modelForm.withError(RecaptchaVerifier.formErrorKey, RecaptchaErrorCode.responseMissing), 
                    	fieldName = "myCaptcha"))
            
            // no error passed to recaptcha
            html must contain(s"$scriptApi?k=public-key")
            html must contain(s"$noScriptApi?k=public-key")
            
            // error.required (in french) shown to end user
            html must contain("<dd class=\"error\">Fr-Error-Required</dd>")
        }
        
        "treat captchaIncorrect as external, showing error.captchaIncorrect" in new WithApplication(validApplication) {
            val html = contentAsString(views.html.recaptcha.recaptchaField(
                    form = modelForm.withError(RecaptchaVerifier.formErrorKey, RecaptchaErrorCode.captchaIncorrect), 
                    	fieldName = "myCaptcha"))
            
            // error passed to recaptcha
            html must contain(s"$scriptApi?k=public-key&error=incorrect-captcha-sol")
            html must contain(s"$noScriptApi?k=public-key&error=incorrect-captcha-sol")
            
            // error.captchaIncorrect (in french) shown to end user
            html must contain("<dd class=\"error\">Fr-Error-CaptchaIncorrect</dd>")
        }
        
        "treat recaptchaNotReachable as internal, showing error.recaptchaNotReachable" in new WithApplication(validApplication) {
            val html = contentAsString(views.html.recaptcha.recaptchaField(
                    form = modelForm.withError(RecaptchaVerifier.formErrorKey, RecaptchaErrorCode.recaptchaNotReachable), 
                    	fieldName = "myCaptcha"))
            
            // no error passed to recaptcha
            html must contain(s"$scriptApi?k=public-key")
            html must contain(s"$noScriptApi?k=public-key")
            
            // error.recaptchaNotReachable (in french) shown to end user
            html must contain("<dd class=\"error\">Fr-Error-RecaptchaNotReachable</dd>")
        }
        
        "treat apiError as internal, showing error.apiError" in new WithApplication(validApplication) {
            val html = contentAsString(views.html.recaptcha.recaptchaField(
                    form = modelForm.withError(RecaptchaVerifier.formErrorKey, RecaptchaErrorCode.apiError), 
                    	fieldName = "myCaptcha"))
            
            // no error passed to recaptcha
            html must contain(s"$scriptApi?k=public-key")
            html must contain(s"$noScriptApi?k=public-key")
            
            // error.apiError (in french) shown to end user
            html must contain("<dd class=\"error\">Fr-Error-ApiError</dd>")
        }
        
        "pass tabindex to recaptcha widget" in new WithApplication(validApplication) {
            val html = contentAsString(views.html.recaptcha.recaptchaField(
                    form = modelForm, fieldName = "myCaptcha", tabindex = Some(21)))
            
            // no error passed to recaptcha
            html must contain(s"$scriptApi?k=public-key")
            html must contain(s"$noScriptApi?k=public-key")
            
            // no error shown to end user
            html must not contain("<dd class=\"error\">")
            
            // constraint.required (in french) shown to end user
            html must contain("<dd class=\"info\">Fr-Constraint-Required</dd>")
            
            // must have options with tabindex 
            html must contain("RecaptchaOptions")
            html must contain("tabindex : 21")
        }
    }
}