Play reCAPTCHA Module
=====================

This is a [Play Framework](http://www.playframework.com) module, for Scala 2.10 and Play 2.3, to provide integration 
with [Google reCAPTCHA](http://www.google.com/recaptcha) in a reactive (non-blocking) manner.

How to use
----------
Please see this [example application](http://www.github.com/chrisnappin/play-recaptcha-example) for details of
how to use this module in your own Scala Play 2.x web applications. You can download this code and run it in Play
(version 2.3 and above), or you can follow the instructions below to add this module to an existing web application.

Adding play-recaptcha to an existing application
------------------------------------------------
The [play-recaptcha](https://github.com/chrisnappin/play-recaptcha) module comes with two APIs:
* [High Level API](high-level-api.md) - that integrates with Play's Form APIs
* [Low Level API](low-level-api.md) - that has no such dependency

Unless you are using a non-standard approach in your Play 2 Scala applications, the High Level API is the one to use - 
it is much simpler and requires much less code.

License
-------

The Play reCAPTCHA Module is copyright Chris Nappin, and is released under the 
[Apache 2 License](http://www.apache.org/licenses/LICENSE-2.0).

Trademarks
----------
Google and possibly reCAPTCHA are trademarks of Google Inc.

