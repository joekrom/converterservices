# converterservices

In this project a bunch of tools, especially for XML publication, is collected and wrapped in a Java toolbox for simple access.

*converterservices* can be used as library, can be started as web service, using [sparkjava](http://sparkjava.com/) as framework,
or (possibly to be implemented) as a command line tool for pipeline processing.

Check out the [Wiki] (https://github.com/axxepta/converterservices/wiki) to find out more about *converterservices*.

Some wrapped external tools have to be installed independently and made accessible (ImageMagick, exiftool) if they shall be used,
but most of the functionality makes use of open source Java libraries which can be bundled in a single JAR (using the Gradle fatJar task).

**converterservices is a WORK IN PROGRESS!**

**NOTE:** As *converterservices* is a toolbox it heavily makes use of static functions.
Nevertheless, the classes have no private constructors because one of our scenarios of use is calling the functions in a BaseX XQuery
environment by Java Binding which demands public constructors.