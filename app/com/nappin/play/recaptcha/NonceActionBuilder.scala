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

import javax.inject.{Inject, Singleton}

import play.api.Logger
import play.api.libs.typedmap.TypedKey
import play.api.mvc.{Action, ActionBuilderImpl, BodyParsers, Request, Result}
import play.filters.headers.SecurityHeadersFilter

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

/** Custom action builder to handle Content Security Policy (CSP) Nonces (Numbers used once).
  *
  * @author
  *   chrisnappin
  * @param settings
  *   Recaptcha configuration settings
  * @param nonceGenerator
  *   The nonce generator
  * @param parser
  *   The body parser to use
  */
class NonceActionBuilder @Inject() (
    settings: RecaptchaSettings,
    nonceGenerator: NonceGenerator,
    parser: BodyParsers.Default
)(implicit ec: ExecutionContext)
    extends ActionBuilderImpl(parser):

  /** The logger to use. */
  private val logger = Logger(this.getClass)

  /** Allows composition with other <code>ActionBuilder</code>'s.
    * @param action
    *   The action
    * @tparam A
    *   The request content type
    * @return
    *   The composed action
    */
  def apply[A](action: Action[A]): Action[A] = async(action.parser) { request =>
    logger.debug("in apply")
    action(request)
  }

  /** Adds a generated nonce to the request (as a request attribute), and adds a corresponding
    * policy to the response.
    * @param request
    *   The current request
    * @param block
    *   The action to execute
    * @tparam A
    *   The request content type
    * @return
    *   The result
    */
  override def invokeBlock[A](
      request: Request[A],
      block: (Request[A]) => Future[Result]
  ): Future[Result] =
    logger.debug("In invokeBlock")

    val nonce: String = nonceGenerator.generateNonce
    logger.info("Using nonce " + nonce)

    // add the nonce to the request as an attribute
    val newRequest = request.addAttr(NonceRequestAttributes.Nonce, nonce)

    val policy = settings.contentSecurityPolicy.replaceAll("\\{nonce\\}", nonce)

    // run the action and add the policy header to the response
    block(newRequest).map(
      _.withHeaders(SecurityHeadersFilter.CONTENT_SECURITY_POLICY_HEADER -> policy)
    )

/** The request attribute used to store the nonce. */
object NonceRequestAttributes:
  val Nonce: TypedKey[String] = TypedKey("nonce")

@Singleton
class NonceGenerator @Inject() (settings: RecaptchaSettings):

  /** The random number generator to use. */
  private val random = initialiseRandom()

  /** Get a nonce value, of the configured length.
    * @return
    *   Random numbers and upper/lower case alphabetic characters
    */
  def generateNonce: String =
    random.alphanumeric.take(settings.nonceLength).mkString

  /** Initialises the random number generator, possibly with a seed.
    * @return
    *   The generator
    */
  private def initialiseRandom(): Random =
    val seed: Option[Long] = settings.nonceSeed
    if seed.isDefined then
      new Random(seed.get)
    else
      new Random()
