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

import play.api.Configuration
import play.api.libs.ws.WSClient

/**
  * Injection helper for <code>RecaptchaComponents</code>.
  *
  * @author chrisnappin, gmalouf
  */
trait RecaptchaComponents {
  def configuration: Configuration
  def wsClient: WSClient

  lazy val recaptchaSettings = new RecaptchaSettings(configuration)
  lazy val responseParser = new ResponseParser()
  lazy val recaptchaVerifier = new RecaptchaVerifier(recaptchaSettings, responseParser, wsClient)
  lazy val recaptchaWidgetHelper = new WidgetHelper(recaptchaSettings)
}
