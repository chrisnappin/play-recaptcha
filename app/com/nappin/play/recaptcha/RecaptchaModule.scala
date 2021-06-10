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

import play.api.inject.Module
import play.api.{Environment, Configuration}

/**
 * Recaptcha Play DI Module.
 *
 * @author chrisnappin, gmalouf
 */
class RecaptchaModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = {
    Seq(
      bind[RecaptchaSettings].toSelf.eagerly(),
      bind[ResponseParser].toSelf.eagerly(),
      bind[RecaptchaVerifier].toSelf.eagerly(),
      bind[WidgetHelper].toSelf.eagerly()
    )
  }
}
