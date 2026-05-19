# Axis2 XSLT Message Formatter
This project contains an Axis2 `MessageFormatter` implementation that will create a HTML or plain text output by applying the XML Style Sheet included in the SOAP Header to the first element of the SOAP Body.

___
Code hosted at https://github.com/holodeck-b2b/axis2-xsltformatter.git  
Issue tracker https://github.com/holodeck-b2b/axis2-xsltformatter/issues  

## Usage
### Installation
The formatter can be installed in any Axis2 version 1.8.2 or later deployment running Java 11 or later. To be able to use the `XSLTFormatter` put the project's jar file in your Axis2 deployment's `lib` directory. For best XSLT support it is recommend to also install the Saxon XSLT processor. You can download [its jar file from the Maven central repository](https://repo1.maven.org/maven2/net/sf/saxon/Saxon-HE).

### Creating HTML output 
The `XSLTFormatter` will create either HTML or plain text output by transforming the XML contained in the [first element of the] SOAP Body of the message to a new document using the XML Style Sheet provided in the SOAP header. To use it make sure that the `org.holodeckb2b.axis2.HTMLFormatter` is used to format the message. 
This can be done either by setting the formatter explicitly in the message context property `org.apache.axis2.Constants.Configuration.MESSAGE_FORMATTER` or by configuring it in the Axis2 configuration and have it selected based on the message's type (as specified in the `org.apache.axis2.Constants.Configuration.MESSAGE_TYPE` message context property). 

The formatter reads the XSLT that must be applied from the `stylesheet` element in the SOAP header of the message. If the style sheet uses parameters the values for these must be specified in a `parameters` SOAP header block which contains a `parameter` element for each parameter. The `parameter` element must have a `name` attribute specifying the name of the parameter and its text content is used as value during the transformation.

##### Example
Given the following SOAP Envelope 
```xml
<soapenv:Envelope xmlns:soapenv="http://www.w3.org/2003/05/soap-envelope"
    xmlns:xsi="http://www.w3.org/1999/XMLSchema-instance/">
    <soapenv:Header>
        <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
            xmlns:xs="http://www.w3.org/2001/XMLSchema"
            exclude-result-prefixes="xs"
            version="1.0">
            <xsl:output method="html"/>

            <!-- This parameter indicates whether the submission was archived -->
            <xsl:param name="name" />

            <xsl:template match="/">
                <html>
                    <body>
                        <h3>Hello <xsl:value-of select="$name"/>!</h3>
                        <hr/>
                        <div>
                            <xsl:value-of select="//message"/>
                        </div>
                    </body>
                </html>
            </xsl:template>
        </xsl:stylesheet>
        <parameters>
            <parameter name="name">Holodeck B2B</parameter>
        </parameters>
    </soapenv:Header>
    <soapenv:Body>
        <example_doc>
            <message>Look at this awesome email :-)</message>
        </example_doc>
    </soapenv:Body>
</soapenv:Envelope>
```
the resulting HTML document will be:
```html
<html>
	<body>
		<h3>Hello Holodeck B2B!</h3>
		<hr/>
		<div>
		Look at this awesome email :-)
		</div>
	</body>
</html>
```

## Contributing
We are using the simplified Github workflow to accept modifications which means you should:
* create an issue related to the problem you want to fix or the function you want to add (good for traceability and cross-reference)
* fork the repository
* create a branch (optionally with the reference to the issue in the name)
* write your code, don't forget to also update the test code to include tests for new or changed functionality!
* commit incrementally with readable and detailed commit messages
* run the tests to check everything works on compile time and also perform a runtime test
* submit a pull-request against the _next_ branch of this repository

If your contribution is more than a patch, please contact us beforehand to discuss which branch you can best submit the pull request to.

### Submitting bugs
You can report issues directly on the [project Issue Tracker](https://github.com/holodeck-b2b/axis2-mailsender/issues).
Please document the steps to reproduce your problem in as much detail as you can (if needed and possible include screenshots).

## Versioning
Version numbering follows the [Semantic versioning](http://semver.org/) approach.

## License
This module is licensed under the Lesser General Public License V3 (LGPLv3) which is included in the LICENSE in the root of the project.

## Support
Commercial support is provided by Chasquis Consulting. Visit [Chasquis-Consulting.com](http://chasquis-consulting.com) for more information.
