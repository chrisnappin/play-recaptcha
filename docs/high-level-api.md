To add the play-recaptcha module to an existing web application, please follow the instructions below.

* [Pre-requisites](#pre-requisites)
* [Build Dependency](#build-dependency)
* [Configuration](#configuration)
* [Internationalisation](#internationalisation)
* [View Template](#view-template)
* [Messages](#messages)
* [Controller](#controller)
* [Troubleshooting](#troubleshooting)

##Pre-requisites
Before you can use the play-recaptcha module, you need to have registered with 
[Google reCAPTCHA](http://www.google.com/recaptcha) to obtain your **private** (**secret**) and **public** (**site**) keys, which you will need below.

##Build Dependency
The play-recaptcha module is distributed via Maven Central, so you can add the module as a build dependency in SBT. In your top-level *build.sbt* file, add the following:

    libraryDependencies ++= Seq(
      "com.nappin" %% "play-recaptcha" % "2.1" 
    )

The useful `%%` syntax means SBT will select the appropriate binary for your Scala version, and `2.1` is the
play-recaptcha module version number being used - since SBT uses ivy the version number can be alternatively be an expression such as `2.+` (meaning any version 2.x release). 

(see [build.sbt](../build.sbt) for a complete example)


##Configuration
The play-recaptcha module supports the following configuration settings in your *application.conf* file:

Configuration Key|Description|Default Value
-----------------|-----------|-------------
recaptcha.privateKey|Your private (secret) reCAPTCHA key|None
recaptcha.publicKey|Your public (site) reCAPTCHA key|None
recpatcha.requestTimeout|The timeout duration to use when contacting Google reCAPTCHA|10 seconds
recpatcha.theme|The reCAPTCHA widget theme to use (see [reCAPTCHA v2 themes](https://developers.google.com/recaptcha/docs/display#config) for a list of possible values)|light
recaptcha.type|The type of captcha to use (image or audio)|image


(see [application.conf](https://github.com/chrisnappin/play-recaptcha-v2-example/tree/release-2.1/conf/application.conf) for a complete example)

## Internationalisation
This supports a comprehensive number of languages (see [supported language codes](https://developers.google.com/recaptcha/docs/language) for the latest list). Your end users can set a list of preferred languages in their web browser, and these will be identified by Google's JavaScript code itself with no further intervention needed (or interaction with Play i18n).

The [play-recaptcha v2 example application](https://github.com/chrisnappin/play-recaptcha-v2-example/tree/release-2.1) is internationalised to two languages (English, French) as an example.


##View Template
In your view template, you need to include a `recaptcha.recaptchaField` view helper tag within a `form` tag. This will render all of the JavaScript and HTML required for reCAPTCHA, plus by default the `noscript` option for browsers with JavaScript turned off (typically rare these days). Here is a very simple example:

    @import com.nappin.play.recaptcha.WidgetHelper
    
    @(myForm: Form[MyModelObject])(implicit request: Request[AnyContent], messages: Messages, widgetHelper: WidgetHelper)
    
    ..html header..
    
    @helper.form(action = routes.ExampleForm.submitForm()) {
        @helper.inputText(myForm("field1"))
        @helper.inputText(myForm("field2"))
        @recaptcha.recaptchaField(form = myForm, fieldName = "captcha")
    }
    
    ..html footer..

The complete list of `recaptcha.recaptchaField` parameters is as follows (I recommend referencing these by name):
* Explicit parameters:
  * ``form: Form[_]`` - the Play Form
  * ``fieldName: String`` - the name of the field
  * ``tabindex: Option[Int]`` - the HTML tabindex, default is ``None``
  * ``includeNoScript: Boolean`` - whether to support non-JavaScript clients, default is ``true`` (used by reCAPTCHA v2 only)
  * ``isRequired: Boolean`` - whether to show the Play ``constraint.required`` message, default is ``false`` (note that the recaptcha field is always processed as if having a **required** form validation constraint)
* Implicit parameters:
  * ``request: Request[AnyContent]`` - the current web request
  * ``messages: Messages`` - the current i18n messages to use
  * ``widgetHelper: WidgetHelper`` - the widgetHelper to use

(see [form.scala.html](https://github.com/chrisnappin/play-recaptcha-v2-example/tree/release-2.1/app/views/form.scala.html) for a complete example)


##Messages
If defined, play-recaptcha supports the following message keys:

Message Key|Description|Default Message
-----------|-----------|---------------
as defined by the parameter to ``recaptchaField`` tag|The field label|None
error.required|Shown if no text entered into the captcha field|The Play default
error.recaptchaNotReachable|Shown if there is an error contacting the reCAPTCHA API (e.g. Network timeout)|Unable to contact Recaptcha
error.apiError|Shown if reCAPTCHA returns a response that the play-recaptcha module doesn't understand (e.g. Google have changed its functionality)|Invalid response from Recaptcha

(see [messages](https://github.com/chrisnappin/play-recaptcha-v2-example/tree/release-2.1/conf/messages) for a complete example)


##Controller
Within your controller, you simply inject a verifier, and an implicit widgetHelper. An example using the built-in Guice DI would be:

    import com.nappin.play.recaptcha.{RecaptchaVerifier, WidgetHelper}
    import javax.inject.Inject
    
    class ExampleForm @Inject() (val messagesApi: MessagesApi, val verifier: RecaptchaVerifier)(
        implicit widgetHelper: WidgetHelper) extends Controller with I18nSupport {

Note that the play-recaptcha module performs a sanity check of the configuration upon startup. If it finds a fatal error it will write details of the issues to the error log, throw an unchecked ``com.typesafe.config.ConfigException`` or ``play.api.PlayException`` and the DI injection will fail. These are errors you should only get whilst developing your module.

To show the form, invoke the template as you would normally, and the implicit ``widgetHelper`` will be passed automatically, for example:

    def show = Action { implicit request =>
        Ok(views.html.form(userForm))
    }

Since the play-recaptcha module uses futures, we also need to have an execution context in scope, e.g.:

    implicit val context = scala.concurrent.ExecutionContext.Implicits.global

Then in your form action, use the verifier's ``bindFromRequestAndVerify`` method much as you would a form's ``bindFromRequest`` method, except it returns a ``Future[Form[T]]`` instead of a ``Form[T]`` instance.  Your form action can use this to return an asynchronous response, allowing the Play Framework to process further requests on the current thread, and reactively process the reCAPTCHA response once it is received. 

The ``bindFromRequestAndVerify`` method has the following parameters:

* explicit parameters:
  * ``form: Form[T]`` - the form to use
* implicit parameters:
  * ``request: Request[AnyRequest]`` - the current request
  * ``context: ExecutionContext`` - used for future execution

The ``bindFromRequestAndVerify`` method does the following:

1. Binds the form data from the request, applying any form validation you have defined. Also reads the captcha field data from the request, checking the user has entered something
  1. If this fails you are returned a ``Future`` containing a ``Form`` with errors
1. If all is ok, the the reCAPTCHA API is invoked reactively to verify the user's captcha response, and a ``Future`` is returned back to your code
1. When the reCAPTCHA API responds, the result is asynchronously processed
  1. If this fails the ``Future`` is populated with a ``Form`` with errors (e.g. captcha was invalid, timeout, or an API error)
  1. If all is ok the ``Future`` is populated with a ``Form`` with no errors

Note that reCAPTCHA isn't invoked if your own form validation fails, or the user doesn't enter a captcha response (so nothing to verify).

This method can also throw ``IllegalStateException`` if it encounters odd situations like no captcha field present on the form that POST-ed the response. You shouldn't need to explicitly catch this exception, as these are fatal errors you should only get if you've made a development error (you test your application thoroughly, right?).

The ``map`` and ``fold`` methods are a great way of handling the ``Future`` and ``Form`` errors, so here is a simple example:

    def submitForm = Action.async { implicit request =>
        implicit val context = scala.concurrent.ExecutionContext.Implicits.global
        
        verifier.bindFromRequestAndVerify(myForm).map { form =>
            form.fold(   
                errors => {
                    // form validation or captcha test failed
                    // so do something like redisplay the current form
                },  
                success => {
                    // form validation and captcha passed
                    // so do some processing then maybe redirect to a success screen
                }
            )
        }    
    }

(see [ExampleForm.scala](https://github.com/chrisnappin/play-recaptcha-v2-example/tree/release-2.0/app/controllers/ExampleForm.scala) for a complete example)


##Troubleshooting
As mentioned above, the play-recaptcha module checks the application configuration upon startup, and if it finds some fatal problems it will throw an unchecked exception. If this happens you should see a message like the following in your application logs:

    15:26:24.501 ERROR com.nappin.play.recaptcha.RecaptchaSettings recaptcha.privateKey not found in application configuration
    15:26:24.508 ERROR com.nappin.play.recaptcha.RecaptchaSettings Mandatory configuration missing. Please check the module documentation and add the missing items to your application.conf file.

The play-recaptcha module uses the Play logging mechanism to log any errors it encounters. To see more details, you can explicitly set the log level for the `com.nappin.play.recaptcha` package as follows:
* **info** - Logs details of captcha challenges and responses, as a brief summary for each call
* **debug** - Logs lots of details of what it is doing

An example logback configuration setting to add to your *logback.xml* file would be as follows:

    <logger name="com.nappin.play.recaptcha" level="debug"/>