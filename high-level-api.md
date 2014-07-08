Play reCAPTCHA - High Level API
===============================

To add the play-recaptcha module to an existing web application, please follow the instructions below.

Pre-requisites
--------------
Before you can use the play-recaptcha module, you need to have registered with 
[Google reCAPTCHA](http://www.google.com/recaptcha) to obtain your **private** and **public** keys, which you will 
need below.

Build Dependency
----------------
The play-recaptcha module is distributed via Maven Central, so you can add the module as a build dependency in SBT. 
In your top-level *build.sbt* file, add the following:

    libraryDependencies ++= Seq(
      "com.nappin.play" %% "play-recaptcha" % "0.1-SNAPSHOT" 
    )

The useful `%%` syntax means SBT will select the appropriate binary for your Scala version, and `0.1-SNAPSHOT` is the
play-recaptcha module version number to use - since SBT uses ivy the version number can be alternatively be an 
expression such as `1.+`. 

Configuration
-------------
The following configuration settings must be added to your *application.conf* file:

* `recaptcha.privateKey` - this is your private reCAPTCHA key
* `recaptcha.publicKey` - this is your public reCAPTCHA key

Optionally, you can also have the following extra configuration settings:

* `recpatch.requestTimeout` - the timeout duration to use when contacting Google reCAPTCHA. The default is **10 seconds**



View Template Code Changes
--------------------------
**TODO**

Messages
--------
**TODO**

Controller Code Changes
-----------------------
**TODO**

Troubleshooting
---------------
The play-recaptcha module checks the application configuration upon startup, and if it finds some fatal problems it will
refuse to startup. If this happens you should see a message like the following in your application logs:

**put example here**

The play-recaptcha module uses the Play logging mechanism to log any errors it encounters. To see more details, you
can explicitly set the log level for the `com.nappin.play.recaptcha` package as follows:
* **info** - Logs details of captcha challenges and responses, as a brief summary for each call
* **debug** - Logs lots of details of what it is doing

An example logback configuration setting to add to your *logger.xml* file would be as follows:

    <logger name="com.nappin.play.recaptcha" level="debug"/>


