# converterservices

A set of wrappers (steps) for the conversion of commonly used data formats (csv, json, excel, xml)

The list of supported steps contains

* Saxon XSLT
* Saxon XQuery
* FOP XSL-FO
* PDF splitting and merging (pdfbox)
* XSLX/CSV <-> XML (Apache POI)
* Zipping/unzipping
* md5 hashing
* exiftool meta data extraction
* Upload and download by FTP and HTTP


*converterservices* can be used as library, can be started as web service, using [sparkjava](http://sparkjava.com/) as framework,
or as a command line tool for pipeline processing with error reporting (you can config the logger to send the reports to
a remote server logging instance or by email).

*converterservices* requires Java 8.

Check out the [Wiki](https://github.com/axxepta/converterservices/wiki) to find out more about *converterservices*.

Some wrapped external tools have to be installed independently and made accessible (ImageMagick, exiftool) if they shall be used,
but most of the functionality makes use of open source Java libraries which can be bundled in a single JAR (using the Gradle fatJar task).

**converterservices is a WORK IN PROGRESS!**

**NOTE:** As *converterservices* is a toolbox it heavily makes use of static functions.
Nevertheless, the classes have no private constructors because one of our scenarios of use is calling the functions in a BaseX XQuery
environment by Java Binding which demands public constructors.
