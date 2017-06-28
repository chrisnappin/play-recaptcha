This is a [Play Framework](http://www.playframework.com) module, for Scala and Play 2.x, to provide integration with [Google reCAPTCHA](http://www.google.com/recaptcha) in a reactive (non-blocking) manner.

* [Release details](#module-release)
* [Changelog](#changelog)
* [Module Dependency](#module-dependency)
* [How to use](#how-to-use)

## Module Release

| Module Revision | reCAPTCHA Versions | Play Version | Scala Versions | ScalaDoc | 
|:---------------:|:------------------:|:------------:|:--------------:|:--------:|
|2.2              |v2, invisible       |2.5.x         |2.11            |[ScalaDoc](http://www.javadoc.io/doc/com.nappin/play-recaptcha_2.11/2.2)|

### Changelog

#### Release 2.2
* Added support for invisible reCAPTCHA
* Added support for AJAX form submission
* Added support for optional HTML attribute arguments
* Refactored recaptchaField to simplify alternative HTML field layouts

Many thanks to @varju for the pull request!

#### Release 2.1
* Added extra customisations for reCAPTCHA v2 language, tabindex and size options
* Removed support for reCAPTCHA v1, since Google no longer supports it or issues new API keys, and it helped greatly simplify the codebase.

#### Release 2.0
* Added support for Play 2.5.x

_Another version jump_ because the API has changed to accommodate Play no longer having any static configuration. As before, the functionality and the rest of the APIs have not changed at all.

Many thanks to @gmalouf and @akitaylor for the pull requests!

#### Release 1.5
* Added support for Play 2.4.x (converted from a Plugin to a DI Module)

_Why the version number jump?_ Well, the way the module is used (injected via DI) has changed, and the APIs have been updated to use Play 2.4 i18n, so this release is only compatible with Play 2.4. However, the functionality and the rest of the APIs have not changed at all.  

#### Release 1.0
* Added support for reCAPTCHA version 2 (aka no-captcha reCAPTCHA)

#### Release 0.9 
* Added full support for internationalisation (Play i18n and reCAPTCHA language and custom strings)
* Added security settings (invoke service and widget using HTTP or HTTPS)

#### Release 0.8
* Initial release

## Module Dependency
The play-recaptcha module is distributed using Maven Central so it can be easily added as a library dependency in your Play Application's SBT build scripts, as follows:

    "com.nappin" %% "play-recaptcha" % "2.2"

## How to use
Please see this example:

![reCAPTCHA version 2 example](recaptcha-example-v2.png)

* [example reCAPTCHA v2 application](https://github.com/chrisnappin/play-recaptcha-v2-example/tree/release-2.2) 

for a ready-to-use internationalised Scala Play 2.x web application using this module, using reCAPTCHA v2 and Invisible reCAPTCHA, with Play Forms and with JavaScript/AJAX. You can download this code and run it in Play, or you can follow the instructions below to add this module to an existing web application.

The play-recaptcha module comes with two APIs:
* [High Level API](high-level-api.md) - that integrates with Play's Form APIs
* [Low Level API](low-level-api.md) - that has no such dependency

Unless you are using a non-standard approach in your Play 2 Scala applications, the High Level API is the one to use - it is much simpler and requires much less code.