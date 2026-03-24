/*
 * Copyright (C) 2026 The Holodeck B2B Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/&gt;.
 */
package org.holodeckb2b.axis2;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.xml.transform.stream.StreamSource;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.kernel.TransportUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link XSLTFormatter} class
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
class XSLTFormatterTest {
    private XSLTFormatter formatter = new XSLTFormatter();

    private static final OMElement XSLT;
    private static final OMElement PARAMS;
    private static final OMElement XML_INPUT;

    static {
		String xsltText = "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\""
				+ "            xmlns:xs=\"http://www.w3.org/2001/XMLSchema\""
				+ "            exclude-result-prefixes=\"xs\""
				+ "            version=\"1.0\">"
				+ "            <xsl:output method=\"html\"/>"
				+ "            <xsl:param name=\"name\" />"
				+ "            <xsl:template match=\"/\">"
				+ "                <html>"
				+ "                    <body>"
				+ "                        <h3>Hello <xsl:value-of select=\"$name\"/>!</h3>"
				+ "                        <hr/>"
				+ "                        <div>"
				+ "                            <xsl:value-of select=\"//message\"/>"
				+ "                        </div>"
				+ "                    </body>"
				+ "                </html>"
				+ "            </xsl:template>"
				+ "        </xsl:stylesheet>";
		String paramText = "<parameters>"
				+ "            <parameter name=\"name\">Holodeck B2B</parameter>"
				+ "        </parameters>";
		String inputText = "<example_doc>"
				+ "            <message>Look at this awesome email :-)</message>"
				+ "        </example_doc>";

    	try (ByteArrayInputStream xsltStream = new ByteArrayInputStream(xsltText.getBytes());
			 ByteArrayInputStream paramStream = new ByteArrayInputStream(paramText.getBytes());
    		 ByteArrayInputStream xmlInputStream = new ByteArrayInputStream(inputText.getBytes())) {
    		XSLT = OMXMLBuilderFactory.createOMBuilder(new StreamSource(xsltStream)).getDocument().getOMDocumentElement();
    		XSLT.build();
    		PARAMS = OMXMLBuilderFactory.createOMBuilder(new StreamSource(paramStream)).getDocument().getOMDocumentElement();
    		PARAMS.build();
    		XML_INPUT = OMXMLBuilderFactory.createOMBuilder(new StreamSource(xmlInputStream)).getDocument().getOMDocumentElement();
    		XML_INPUT.build();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
    }

    @Test
    void testGetContentType() throws AxisFault {
		SOAPEnvelope soapEnvelope = TransportUtils.createSOAPEnvelope(XML_INPUT);
		soapEnvelope.getHeader().addChild(XSLT);

		MessageContext messageContext = new MessageContext();
        messageContext.setEnvelope(soapEnvelope);

        assertEquals("text/html", formatter.getContentType(messageContext, null, null));
    }

    @Test
    void testFormatSOAPAction() {
        // Test that formatSOAPAction returns null
        MessageContext messageContext = new MessageContext();
        messageContext.setSoapAction("testAction");
        assertNull(formatter.formatSOAPAction(messageContext, null, null));
    }

    @Test
    void testWriteToMissingStylesheet() throws Exception {
        // Test that the formatter throws an AxisFault when no XSLT stylesheet is provided
        MessageContext messageContext = new MessageContext();
        messageContext.setEnvelope(TransportUtils.createSOAPEnvelope(XML_INPUT));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        	assertThrows(AxisFault.class, () -> formatter.writeTo(messageContext, null, out, false));
        }
    }

    @Test
    void testTransform() throws Exception {
		SOAPEnvelope soapEnvelope = TransportUtils.createSOAPEnvelope(XML_INPUT);
		soapEnvelope.getHeader().addChild(XSLT);
		soapEnvelope.getHeader().addChild(PARAMS);

		MessageContext messageContext = new MessageContext();
		messageContext.setEnvelope(soapEnvelope);

		byte[] output;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        	assertDoesNotThrow(() -> formatter.writeTo(messageContext, null, out, false));
        	output = out.toByteArray();
        }

        assertNotNull(output);
        Document result = Jsoup.parse(new String(output));
        assertNotNull(result);

        assertEquals("Hello Holodeck B2B!", result.getElementsByTag("h3").text());
        assertEquals("Look at this awesome email :-)", result.getElementsByTag("div").text());
    }
}