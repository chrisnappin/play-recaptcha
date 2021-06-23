To add the play-recaptcha module to an existing web application, please follow the instructions below.

* [Pre-requisites](#pre-requisites)
* [Build Dependency](#build-dependency)
* [Configuration](#configuration)
* [Internationalisation](#internationalisation)
* [View Template](#view-template)
* [Messages](#messages)
* [Controller](#controller)
* [Troubleshooting](#troubleshooting)

## Pre-requisites
Before you can use the play-recaptcha module, you need to have registered with 
[Google reCAPTCHA](http://www.google.com/recaptcha) to obtain your **private** (**secret**) and **public** (**site**) keys, which you will need below.

Note that if you want to use invisible reCAPTCHA, this is a separate option and you will need separate keys to use it.

## Build Dependency
The play-recaptcha module is distributed via Maven Central, so you can add the module as a build dependency in SBT. In your top-level *build.sbt* file, add the following:

    libraryDependencies ++= Seq(
      "com.nappin" %% "play-recaptcha" % "2.5" 
    )

The useful `%%` syntax means SBT will select the appropriate binary for your Scala version, and `2.5` is the
play-recaptcha module version number being used - since SBT uses ivy the version number can be alternatively be an expression such as `2.+` (meaning any version 2.x release). 

(see [build.sbt](../build.sbt) for a complete example)


## Configuration
The play-recaptcha module supports the following configuration settings in your *application.conf* file:

Configuration Key|Description|Default Value
-----------------|-----------|-------------
recaptcha.privateKey|Your private (secret) reCAPTCHA key|None
recaptcha.publicKey|Your public (site) reCAPTCHA key|None
recpatcha.requestTimeout|The timeout duration to use when contacting Google reCAPTCHA|10 seconds
recpatcha.theme|The reCAPTCHA widget theme to use (see [reCAPTCHA v2 themes](https://developers.google.com/recaptcha/docs/display#config) for a list of possible values)|light
recaptcha.type|The type of captcha to use (image or audio)|image
recaptcha.size|The size of captcha to show (normal or compact)|normal
recaptcha.languageMode|The internationalisation approach to use (auto, force, play)|auto
recaptcha.forceLanguage|The language to use (if using force mode)|None
recaptcha.nonceAction.contentSecurityPolicy|The policy to use (if using NonceActionBuilder)|See below
recaptcha.nonceAction.nonceLength|The length of nonces to generate|20
recaptcha.nonceAction.nonceSeed|The seed to use (if any)|None


(see [application.conf](https://github.com/chrisnappin/play-recaptcha-v2-example/tree/release-2.5/conf/application.conf) for a complete example)

## Internationalisation
The play-recaptcha module (both reCAPTCHA v2 and Invisible reCAPTCHA) supports the following language modes (as set by the `recaptcha.languageMode` configuration setting:
* `auto` - uses Google's JavaScript and built-in language support (see [supported language codes](https://developers.google.com/recaptcha/docs/language)) to automatically render the reCAPTCHA widget in the end user's web browser's preferred language, with no interaction with Play i18n.
* `force` - forces the reCAPTCHA widget to use the language defined by the `recapctha.forceLanguage` configuration setting.
* `play` - renders the reCAPTCHA widget using the Play i18n locale (which you can set in your application or use the default Play behaviour)

Unless you need special behaviour (e.g. your website has its own language selection functionality, or it is only ever rendered in one language), use the default `auto` mode.

The [play-recaptcha v2 example application](https://github.com/chrisnappin/play-recaptcha-v2-example/tree/release-2.5) is internationalised to two languages (English, French) as an example.


## View Template

### reCAPTCHA v2
To use reCAPTCHA version 2 in your view template, you need to inject a `recaptcha.recaptchaField` view helper tag and include it within a `form` tag. This will render all of the JavaScript and HTML required for reCAPTCHA, and optionally the `noscript` option for browsers with JavaScript turned off (typically rare these days). Here is a very simple example:

    @this(recaptchaField: recaptcha.recaptchaField)
    
    @(myForm: Form[MyModelObject])(implicit messagesProvider: MessagesProvider, request: Request[AnyContent])
    
    ..html header..
    
    @helper.form(action = routes.ExampleForm.submitForm) {
        @helper.inputText(myForm("field1"))
        @helper.inputText(myForm("field2"))
        @recaptchaField(form = myForm, fieldName = "captcha", includeNoScript = false, isRequired = true, 
            Symbol("class") -> "extraClass")
            
        ..further fields and a submit button..    
    }
    
    ..html footer..

The `recaptchaField` parameters are as follows:
* Explicit parameters:
  * ``form: Form[_]`` - the Play Form
  * ``fieldName: String`` - the name of the field
  * ``tabindex: Int`` - the HTML tabindex
  * ``includeNoScript: Boolean`` - whether to support non-JavaScript clients
  * ``isRequired: Boolean`` - whether to show the Play ``constraint.required`` message (note that the recaptcha field is always processed as if having a **required** form validation constraint)
  * ``args: (Symbol, String)*`` - optional HTML attributes to add to the recaptcha div (like the built-in Play input helpers)
* Implicit parameters:
  * ``messagesProvider: MessagesProvider`` - the current i18n messages to use
  * ``request: Request[AnyContent]`` - the current request

(see [form.scala.html](https://github.com/chrisnappin/play-recaptcha-v2-example/tree/release-2.5/app/views/form.scala.html) for a complete example)

The `recaptchaField` view helper tag (ignoring the JavaScript) produces HTML using the same formatting as the built-in Play input helpers, as follows:

    <dl ...>
      <dt><label for="..field..">..label..</label></dt>
      <dd><div class="g-recaptcha" ..>..the reCAPTCHA widget..</div>..no script block etc..</dd>
      <dd class="error">..any errors..</dd>
      <dd class="info">..any constraints..</dd>
    </dl>

You can use the optional HTML attributes (`args`) to add CSS classes and further attributes to the `g-recaptcha` div to style/layout the reCAPTCHA widget.

If you are using a third-party library to layout your form (for example, Bootstrap), you will probably want to replace use of the dl/dt/dd HTML with custom div tags. To achieve this, copy `recaptchaField.scala.html` into your own project, reference that in your form template, then change the HTML as desired.
 
### Invisible reCAPTCHA
To use Invisible reCAPTCHA in your view template, you need to inject a `recaptcha.invisibleButton` view helper tag and include it within a `form` tag. This will render all of the JavaScript and HTML required for Invisible reCAPTCHA. Here is a very simple example:

    @this(invisibleButton: recaptcha.invisibleButton)
    
    @(myForm: Form[MyModelObject])(implicit messagesProvider: MessagesProvider, request: Request[AnyContent])
    
    ..html header..
    
    @helper.form(action = routes.ExampleForm.submitForm, Symbol("id") -> "my_form") {
        @helper.inputText(myForm("field1"))
        @helper.inputText(myForm("field2"))
        @invisibleButton(formId = "my_form", text = "Submit", Symbol("class") -> "extraClass")
    }
    
    ..html footer..

The `invisibleButton` parameters are as follows:
* Explicit parameters:
  * ``formId: String`` - the id for the Form
  * ``text: String`` - the text of the submit button
  * ``args: (Symbol, String)*`` - optional HTML attributes to add to the button (like the built-in Play input helpers)
* Implicit parameters:
  * ``messagesProvider: MessagesProvider`` - the current i18n messages to use
  * ``request: Request[AnyContent]`` - the current web request

(see [invisibleForm.scala.html](https://github.com/chrisnappin/play-recaptcha-v2-example/tree/release-2.5/app/views/invisibleForm.scala.html) for a complete example)

The `invisibleButton` view helper tag (ignoring the JavaScript) produces HTML as follows:

    <button class="g-recaptcha" ..>..button text..</button>

You can use the optional HTML attributes (`args`) to add CSS classes and further attributes to the button for styling/layout.


## Messages
If defined, play-recaptcha supports the following message keys:

Message Key|Description|Default Message
-----------|-----------|---------------
as defined by the parameter to ``recaptchaField`` tag|The field label|None
error.required|Shown if no text entered into the captcha field|The Play default
error.recaptchaNotReachable|Shown if there is an error contacting the reCAPTCHA API (e.g. Network timeout)|Unable to contact Recaptcha
error.apiError|Shown if reCAPTCHA returns a response that the play-recaptcha module doesn't understand (e.g. Google have changed its functionality)|Invalid response from Recaptcha

(see [messages](https://github.com/chrisnappin/play-recaptcha-v2-example/tree/release-2.5/conf/messages) for a complete example)


## Controller

### reCAPTCHA v2 (using Play forms)
Within your controller, you simply inject a verifier, your view template (so that template dependencies are injected), and an implicit `ExecutionContext`. An example using the built-in dependency injection would be:

    import com.nappin.play.recaptcha.RecaptchaVerifier
    import javax.inject.Inject
    
    class ExampleForm @Inject() (myForm: views.html.myForm, verifier: RecaptchaVerifier, 
        cc: ControllerComponents)(implicit executionContext: ExecutionContext) 
            extends AbstractController(cc) with I18nSupport {

Note that the play-recaptcha module performs a sanity check of the configuration upon startup. If it finds a fatal error it will write details of the issues to the error log, throw an unchecked ``com.typesafe.config.ConfigException`` or ``play.api.PlayException`` and the DI injection will fail. These are errors you should only get whilst developing your module.

To show the form, invoke the injected template, for example:

    def show = Action { implicit request: Request[AnyContent] =>
        Ok(myForm(userForm))
    }

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

(see [ExampleForm.scala](https://github.com/chrisnappin/play-recaptcha-v2-example/tree/release-2.5/app/controllers/ExampleForm.scala) for a complete example,
and [ExampleFormSpec.scala](https://github.com/chrisnappin/play-recaptcha-v2-example/tree/release-2.5/test/controllers/ExampleFormSpec.scala) for example unit tests using specs2)


### reCAPTCHA v2 (using AJAX/JavaScript)

To validate a form submitted via JavaScript follows very similar processing as the reCAPTCHA v2 controller, but the ``widgetHelper.resolveRecaptchaErrors`` method can be used to return any form validation errors as JSON.

(see [JavascriptForm.scala](https://github.com/chrisnappin/play-recaptcha-v2-example/tree/release-2.5/app/controllers/JavascriptForm.scala) for a complete example,
see [JavascriptFormSpec.scala](https://github.com/chrisnappin/play-recaptcha-v2-example/tree/release-2.5/test/controllers/JavascriptFormSpec.scala) for example unit tests using specs2)


### Invisible reCAPTCHA

To validate a form with an invisible reCAPTCHA follows the same processing as the reCAPTCHA v2 controller, as outlined above.

(see [InvisibleForm.scala](https://github.com/chrisnappin/play-recaptcha-v2-example/tree/release-2.5/app/controllers/InvisibleForm.scala) for a complete example,
and [InvisibleFormSpec.scala](https://github.com/chrisnappin/play-recaptcha-v2-example/tree/release-2.5/test/controllers/InvisibleFormSpec.scala) for example unit tests using specs2)


## Security

As of Play 2.6, a number of security filters are enabled by default, including the security headers filter which enables Content Security Policy (CSP).

To allow use of reCAPTCHA using the default Play security headers filter, you will need to use a policy such as the following - adding any other external sources your application is using:

```
    default-src 'self'; 
    script-src 'self' 'unsafe-inline' https://www.google.com/recaptcha/ https://www.gstatic.com/recaptcha/; 
    frame-src https://www.google.com/recaptcha/; 
    style-src 'self' 'unsafe-inline'
```

The ```style-src``` directive of ```unsafe-inline``` is needed because of the way the current reCAPTCHA code operates. However, using a ```script-src``` directive of ```unsafe-inline``` is not recommended and undermines use of CSP. A recommended solution is to use SHA digests or nonces, as below.


### CSP Nonce Generation

The play-recaptcha includes a simple nonce generation solution that allows you to use a more strict Content Security Policy. The solution uses a custom action builder that overrides the default filter, so that you can have a very strict policy applied by default on all responses, and a reCAPTCHA-specific policy (broader and using nonces) on responses rendering reCAPTCHA widgets.  

To use the custom action builder, inject it into your controller as follows:

```
class MyForm @Inject()(nonceAction: NonceActionBuilder, ..)(..) 
    extends AbstractController(cc) with I18nSupport {
``` 
 
Then apply it to any actions that render a reCAPTCHA widget as follows:

```
def show = nonceAction { implicit request: Request[AnyContent] =>
  Ok(myTemplate(myForm))
}
```

Or:
 
```
def show = nonceAction.async { 
  ..if returning a Future[..]..
}
``` 
 
Note that you can also compose this custom action builder with other actions as follows:

```
def method = nonceAction { Action {
    ..
  }
}
``` 

```
def method = nonceAction { messagesAction {
    ..
  }
}
``` 

The following configuration will be needed:

Configuration Key|Description|Default Value
-----------------|-----------|-------------
play.filters.headers.contentSecurityPolicy|The default policy used for all other actions|```default-src 'self'```
play.filters.headers.allowActionSpecificHeaders|Must be set to ```true``` for ```nonceAction``` to work|false
recaptcha.nonceAction.contentSecurityPolicy|The policy to use for ```nonceAction```-wrapped actions|See below
recaptcha.nonceAction.nonceLength|The length of nonces to generate|20
recaptcha.nonceAction.nonceSeed|The seed to use (if any)|None

The default Content Security Policy applied by ```nonceAction``` is as follows - override this default to add any other external sources your application is using:
```
    default-src 'self';
    script-src 'self' 'nonce-{nonce}';
    style-src 'self' 'unsafe-inline';
    frame-src https://www.google.com/recaptcha/;
```

where ```{nonce}``` is replaced with a unique nonce generated for each response.

(see [ExampleForm.scala](https://github.com/chrisnappin/play-recaptcha-v2-example/tree/release-2.5/app/controllers/ExampleForm.scala) for a complete example) 

(see [InvisibleForm.scala](https://github.com/chrisnappin/play-recaptcha-v2-example/tree/release-2.5/app/controllers/InvisibleForm.scala) for a complete example with action composition) 


## Troubleshooting
As mentioned above, the play-recaptcha module checks the application configuration upon startup, and if it finds some fatal problems it will throw an unchecked exception. If this happens you should see a message like the following in your application logs:

    15:26:24.501 ERROR com.nappin.play.recaptcha.RecaptchaSettings recaptcha.privateKey not found in application configuration
    15:26:24.508 ERROR com.nappin.play.recaptcha.RecaptchaSettings Mandatory configuration missing. Please check the module documentation and add the missing items to your application.conf file.

The play-recaptcha module uses the Play logging mechanism to log any errors it encounters. To see more details, you can explicitly set the log level for the `com.nappin.play.recaptcha` package as follows:
* **info** - Logs details of captcha challenges and responses, as a brief summary for each call
* **debug** - Logs lots of details of what it is doing

An example logback configuration setting to add to your *logback.xml* file would be as follows:

    <logger name="com.nappin.play.recaptcha" level="debug"/>