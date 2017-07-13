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

import javax.inject.Inject

import com.nappin.play.recaptcha.RecaptchaSettings.{PrivateKeyConfigProp, PublicKeyConfigProp}
import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.runner.JUnitRunner
import org.specs2.specification.Scope
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, MessagesActionBuilder, MessagesRequest, Request}
import play.api.test.{FakeRequest, PlaySpecification, WithApplication}
import play.filters.headers.SecurityHeadersFilter.CONTENT_SECURITY_POLICY_HEADER

import scala.concurrent.{ExecutionContext, Future}

/**
  * Tests the <code>NonceActionBuilder</code> class.
  *
  * @author chrisnappin
  */
@RunWith(classOf[JUnitRunner])
class NonceActionBuilderSpec extends PlaySpecification with Mockito {

  private implicit val context = ExecutionContext.Implicits.global

  private val configuration: Map[String, Any] =  Map(
    PrivateKeyConfigProp -> "private-key",
    PublicKeyConfigProp -> "public-key")

  // TODO: find a way to invoke the Play security filters in the test

  abstract class WithWidgetHelper(configProps: Map[String, Any]) extends WithApplication(
    GuiceApplicationBuilder().configure(configProps).build()) with Scope

  private val request = FakeRequest(GET, "/test")

  "Controller1 using NonceActionBuilder" should {

    "Wrap simple response with no implicit request" in new WithWidgetHelper(configuration) {
      val controller = app.injector.instanceOf[Controller1]
      val response = controller.method1().apply(request)

      status(response) must equalTo(OK)
      contentAsString(response) must contain("response1")

      // response header must contain nonce in the policy
      header(CONTENT_SECURITY_POLICY_HEADER, response).get must =~("script-src 'self' 'nonce-([0-9|a-z|A-Z]{20})'; ")
    }

    "Wrap simple response with implicit request" in new WithWidgetHelper(configuration) {
      val controller = app.injector.instanceOf[Controller1]
      val response = controller.method2().apply(request)

      status(response) must equalTo(OK)

      // response body contains nonce (from request attribute)
      contentAsString(response) must =~("response2 ([0-9|a-z|A-Z]{20})")

      // response header must contain nonce in the policy
      header(CONTENT_SECURITY_POLICY_HEADER, response).get must =~("script-src 'self' 'nonce-([0-9|a-z|A-Z]{20})'; ")
    }

    "Wrap future response without implicit request" in new WithWidgetHelper(configuration) {
      val controller = app.injector.instanceOf[Controller1]
      val response = controller.method3().apply(request)

      status(response) must equalTo(OK)
      contentAsString(response) must equalTo("response3")

      // response header must contain nonce in the policy
      header(CONTENT_SECURITY_POLICY_HEADER, response).get must =~("script-src 'self' 'nonce-([0-9|a-z|A-Z]{20})'; ")
    }

    "Wrap Action without implicit request" in new WithWidgetHelper(configuration) {
      val controller = app.injector.instanceOf[Controller1]
      val response = controller.method4().apply(request)

      status(response) must equalTo(OK)
      contentAsString(response) must equalTo("response4")

      // response header must contain nonce in the policy
      header(CONTENT_SECURITY_POLICY_HEADER, response).get must =~("script-src 'self' 'nonce-([0-9|a-z|A-Z]{20})'; ")
    }

    "Wrap Action with implicit request and nonce config" in new WithWidgetHelper(configuration ++ Map(
          "recaptcha.nonceAction.nonceLength" -> 30,
          "recaptcha.nonceAction.contentSecurityPolicy" -> "custom-policy {nonce}",
          "recaptcha.nonceAction.nonceSeed" -> 123456789012345678L
        )) {
      val controller = app.injector.instanceOf[Controller1]
      val response = controller.method5().apply(request)

      status(response) must equalTo(OK)

      // response body contains nonce (from request attribute)
      contentAsString(response) must =~("response5 ([0-9|a-z|A-Z]{30})")

      // response header must contain nonce in the policy
      header(CONTENT_SECURITY_POLICY_HEADER, response).get must =~("custom-policy ([0-9|a-z|A-Z]{30})")
    }
  }

  "Controller2 using NonceActionBuilder" should {

    "Wrap messages action with no implicit request" in new WithWidgetHelper(configuration) {
      val controller = app.injector.instanceOf[Controller2]
      val response = controller.method1().apply(request)

      status(response) must equalTo(OK)
      contentAsString(response) must contain("response1")

      // response header must contain nonce in the policy
      header(CONTENT_SECURITY_POLICY_HEADER, response).get must =~("script-src 'self' 'nonce-([0-9|a-z|A-Z]{20})'; ")
    }

    "Wrap messages action with implicit request" in new WithWidgetHelper(configuration) {
      val controller = app.injector.instanceOf[Controller2]
      val response = controller.method2().apply(request)

      status(response) must equalTo(OK)

      // response body contains nonce (from request attribute)
      contentAsString(response) must =~("response2 ([0-9|a-z|A-Z]{20})")

      // response header must contain nonce in the policy
      header(CONTENT_SECURITY_POLICY_HEADER, response).get must =~("script-src 'self' 'nonce-([0-9|a-z|A-Z]{20})'; ")
    }

    "Wrap future messages action without implicit request" in new WithWidgetHelper(configuration) {
      val controller = app.injector.instanceOf[Controller2]
      val response = controller.method3().apply(request)

      status(response) must equalTo(OK)
      contentAsString(response) must contain("response3")

      // response header must contain nonce in the policy
      header(CONTENT_SECURITY_POLICY_HEADER, response).get must =~("script-src 'self' 'nonce-([0-9|a-z|A-Z]{20})'; ")
    }

    "Wrap future messages action with implicit request" in new WithWidgetHelper(configuration) {
      val controller = app.injector.instanceOf[Controller2]
      val response = controller.method4().apply(request)

      status(response) must equalTo(OK)

      // response body contains nonce (from request attribute)
      contentAsString(response) must =~("response4 ([0-9|a-z|A-Z]{20})")

      // response header must contain nonce in the policy
      header(CONTENT_SECURITY_POLICY_HEADER, response).get must =~("script-src 'self' 'nonce-([0-9|a-z|A-Z]{20})'; ")
    }
  }
}

/** Simple controller not using any other action builders. */
class Controller1 @Inject()(nonceAction: NonceActionBuilder, cc: ControllerComponents)(implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  def method1 = nonceAction {
    Ok("response1")
  }

  def method2 = nonceAction { implicit request: Request[AnyContent] =>
    Ok("response2 " + request.attrs.get(NonceRequestAttributes.Nonce).get)
  }

  def method3 = nonceAction.async {
    Future {
      Ok("response3")
    }
  }

  def method4 = nonceAction {
    Action {
      Ok("response4")
    }
  }

  def method5 = nonceAction {
    Action { implicit request: Request[AnyContent] =>
      Ok("response5 " + request.attrs.get(NonceRequestAttributes.Nonce).get)
    }
  }

  def method6 = Action {
    Ok("response6")
  }
}

/** Simple controller also using the i18n action builder. */
class Controller2 @Inject()(messagesAction: MessagesActionBuilder, nonceAction: NonceActionBuilder,
  cc: ControllerComponents)(implicit executionContext: ExecutionContext) extends AbstractController(cc) {

  def method1 = nonceAction {
    messagesAction {
      Ok("response1")
    }
  }

  def method2 = nonceAction {
    messagesAction { implicit request: MessagesRequest[AnyContent] =>
      Ok("response2 " + request.attrs.get(NonceRequestAttributes.Nonce).get)
    }
  }

  def method3 = nonceAction {
    messagesAction.async {
      Future {
        Ok("response3")
      }
    }
  }

  def method4 = nonceAction {
    messagesAction.async { implicit request: MessagesRequest[AnyContent] =>
      Future {
        Ok("response4 " + request.attrs.get(NonceRequestAttributes.Nonce).get)
      }
    }
  }

  def method5 = messagesAction {
    Ok("response5")
  }
}