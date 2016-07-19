This is a [Play Framework](http://www.playframework.com) module, for Scala and Play 2.x, to provide integration with [Google reCAPTCHA](http://www.google.com/recaptcha) in a reactive (non-blocking) manner.

## Latest Release

| Module Revision | Play Version | Scala Versions | ScalaDoc | 
|:---------------:|:------------:|:--------------:|:--------:|
|2.0              |2.5.x         |2.11            |[ScalaDoc](http://www.javadoc.io/doc/com.nappin/play-recaptcha_2.11/2.0)|
|1.5              |2.4.x         |2.10, 2.11      |[ScalaDoc](http://www.javadoc.io/doc/com.nappin/play-recaptcha_2.11/1.5)|
|1.0              |2.3.x         |2.10, 2.11      |[ScalaDoc](http://www.javadoc.io/doc/com.nappin/play-recaptcha_2.11/1.0)|

### Changelog

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

##Module Dependency
The play-recaptcha module is distributed using Maven Central so it can be easily added as a library dependency in your Play Application's SBT build scripts, as follows:

    "com.nappin" %% "play-recaptcha" % "2.0"

##How to use
Please see these examples:

![reCAPTCHA version 1 example](https://raw.githubusercontent.com/chrisnappin/play-recaptcha/master/recaptcha-example-v1.png)

reCAPTCHA version 1 
* [example Play 2.5 application](https://github.com/chrisnappin/play-recaptcha-example/tree/release-2.0) 
* [example Play 2.4 application](https://github.com/chrisnappin/play-recaptcha-example/tree/release-1.5) 
* [example Play 2.3 application](https://github.com/chrisnappin/play-recaptcha-example/tree/release-1.0)

![reCAPTCHA version 2 example](https://raw.githubusercontent.com/chrisnappin/play-recaptcha/master/recaptcha-example-v2.png)

reCAPTCHA version 2 
* [example Play 2.5 application](https://github.com/chrisnappin/play-recaptcha-v2-example/tree/release-2.0) 
* [example Play 2.4 application](https://github.com/chrisnappin/play-recaptcha-v2-example/tree/release-1.5)
* [example Play 2.3 application](https://github.com/chrisnappin/play-recaptcha-v2-example/tree/release-1.0)

for ready-to-use internationalised Scala Play 2.x web applications using this module. You can download this code and run it in Play, or you can follow the instructions below to add this module to an existing web application.

The play-recaptcha module comes with two APIs:
* High Level API - that integrates with Play's Form APIs
    * [Play 2.5](High-Level-API-(Play-2.5))
    * [Play 2.4](High-Level-API-(Play-2.4))
    * [Play 2.3](High-Level-API-(Play-2.3))
* Low Level API - that has no such dependency
    * [Play 2.5](Low-Level-API-(Play-2.5))
    * [Play 2.4](Low-Level-API-(Play-2.4))
    * [Play 2.3](Low-Level-API-(Play-2.3)) 

Unless you are using a non-standard approach in your Play 2 Scala applications, the High Level API is the one to use - it is much simpler and requires much less code.