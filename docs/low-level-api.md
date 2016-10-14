The play-recaptcha Low Level API has the same pre-requisites, requirements and configuration as the High level API, but differs as detailed below. To understand this better I recommend reading the [Google reCAPTCHA developer docs](https://developers.google.com/recaptcha/docs/display).

* [View Template](#view-template)
* [Controller](#controller)

##View Template
In your view template, you need to include a ``recaptcha.recaptchaWidget`` view helper tag, which has the following parameters (I recommend referencing these by name):

* Explicit parameters:
  * ``includeNoScript: Boolean`` - whether to support browsers with JavaScript disabled, default is ``true`` (reCAPTCHA version 2 only)
  * ``error: Option[String]`` - the reCAPTCHA error code, default is ``None`` (reCAPTCHA version 1 only)
  * ``tabindex: Option[Int]`` - the HTML tabindex, default is None (reCAPTCHA version 1 only)
* Implicit parameters:
  * ``request: Request[AnyContent]`` - the current web request
  * ``messages: Messages`` - the current i18n messages to use
  * ``widgetHelper: WidgetHelper`` - the widget helper to use (create via DI)

For reCAPTCHA version 1, the error code is passed to the reCAPTCHA API to render the widget, although I've never seen this produce anything visual to help the end user. No messages (labels, error messages etc) are used by the ``recaptcha.recaptchaWidget``, you will need to handle this yourself along with the rest of the HTML for your form field. The custom messages defined in the High Level API as shown by reCAPTCHA are passed by the ``recaptcha.recaptchaWidget``, as is the Play i18n/reCAPTCHA language integration.

##Controller
Within your controller, you simply inject a verifier and an implicit widgetHelper. An example using the built-in Guice DI would be:

    import com.nappin.play.recaptcha.{RecaptchaVerifier, WidgetHelper}
    import javax.inject.Inject

    class ExampleForm @Inject() (val messagesApi: MessagesApi, val verifier: RecaptchaVerifier)(
        implicit widgetHelper: WidgetHelper) extends Controller with I18nSupport {

Note that the play-recaptcha module performs a sanity check of the configuration upon startup. If it finds a fatal error it will write details of the issues to the error log, throw an unchecked ``com.typesafe.config.ConfigException`` or ``play.api.PlayException`` and the DI injection will fail. These are errors you should only get whilst developing your module.

To show the form, invoke the template as you would normally, and the implicit widgetHelper will be passed automatically, for example:

    def show = Action { implicit request =>
        Ok(views.html.form(userForm))
    }

Since the play-recaptcha module uses futures, we also need to have an execution context in scope, e.g.:

    implicit val context = scala.concurrent.ExecutionContext.Implicits.global

Then to check whether a captcha response is valid, use the following methods:

### reCAPTCHA Version 2
Call the ``verifyV2`` method, which has the following parameters:

* explicit parameters:
  * ``response: String`` - The recaptcha response, to verify
  * ``remoteIp: String`` - The IP address of the end user
* implicit parameters:
  * ``context: ExecutionContext`` - used for future execution

### reCAPTCHA Version 1
Call the ``verifyV1`` method, which has the following parameters:

* explicit parameters:
  * ``challenge: String`` - The recaptcha challenge
  * ``response: String`` - The recaptcha response, to verify
  * ``remoteIp: String`` - The IP address of the end user
* implicit parameters:
  * ``context: ExecutionContext`` - used for future execution

### Detailed Processing
Both``verifyV1`` and ``verifyV2`` methods do the following:

1. The reCAPTCHA API is invoked reactively to verify the user's captcha response, and a ``Future`` is returned back to your code
1. When the reCAPTCHA API responds, the result is asynchronously processed
  1. If this fails the ``Future`` is populated with an ``Error`` (e.g. captcha was invalid, timeout, or an API error)
  1. If all is ok the ``Future`` is populated with a ``Success``

Note that it doesn't check whether the ``response`` parameter is empty, it calls reCAPTCHA regardless.

The ``verifyV1`` and ``verifyV2`` methods return a ``Future[Either[Error, Success]]``

An ``Error`` result has a ``code`` property you will need to inspect, which can be the result from reCAPTCHA itself (see [Google reCAPTCHA Error Code Reference](https://developers.google.com/recaptcha/docs/verify)) or one of the internal constants defined in ``RecaptchaErrorCode`` (e.g. ``apiError``).

The ``map`` and ``fold`` methods area great way of handling the ``Future`` and ``Either``, so here is a simple example:

    verifier.verify(challenge, response, remoteIp).map { response =>
        response.fold(   
            error => {
                // captcha incorrect or a technical error
            },  
            success => {
                // captcha passed
            }
        )   
    }