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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.axiom.om.OMOutputFormat;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.kernel.MessageFormatter;
import org.holodeckb2b.commons.xml.AxiomDOMConvertor;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * An Axis2 {@link MessageFormatter} that transforms the XML contained in SOAP Body of the message using XSLT to another
 * XML, HTML or plain text document. It will use the XML Style Sheet provided as child element in the SOAP Header to
 * transform the XML. If the XSLT uses parameters the SOAP header must contain a header block named <code>parameters</code>
 * which includes the values for the parameters in <code>parameter</code> child elements. The <code>parameter</code>
 * elements must have a <code>name</code> to indicate for which XSLT parameter they provide the value.
 * <p>
 * For example the following SOAP Envelope will result in the HTML document shown below.
 * <pre>
&lt;soapenv:Envelope xmlns:soapenv="http://www.w3.org/2003/05/soap-envelope"
    xmlns:xsi="http://www.w3.org/1999/XMLSchema-instance/"&gt;
    &lt;soapenv:Header&gt;
        &lt;xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
            xmlns:xs="http://www.w3.org/2001/XMLSchema"
            exclude-result-prefixes="xs"
            version="1.0"&gt;
            &lt;xsl:output method="html"/&gt;

            &lt;xsl:param name="name" /&gt;

            &lt;xsl:template match="/"&gt;
                &lt;html&gt;
                    &lt;body&gt;
                        &lt;h3&gt;Hello &lt;xsl:value-of select="$name"/&gt;!&lt;/h3&gt;
                        &lt;hr/&gt;
                        &lt;div&gt;
                            &lt;xsl:value-of select="//message"/&gt;
                        &lt;/div&gt;
                    &lt;/body&gt;
                &lt;/html&gt;
            &lt;/xsl:template&gt;
        &lt;/xsl:stylesheet&gt;
        &lt;parameters&gt;
            &lt;parameter name="name"&gt;Holodeck B2B&lt;/parameter&gt;
        &lt;/parameters&gt;
    &lt;/soapenv:Header&gt;
    &lt;soapenv:Body&gt;
        &lt;example_doc&gt;
            &lt;message&gt;Look at this awesome email :-)&lt;/message&gt;
        &lt;/example_doc&gt;
    &lt;/soapenv:Body&gt;
&lt;/soapenv:Envelope&gt;
</pre>
 * Resulting HTML document:
 * <pre>
&lt;html&gt;
	&lt;body&gt;
		&lt;h3&gt;Hello Holodeck B2B!&lt;/h3&gt;
		&lt;hr/&gt;
		&lt;div&gt;
		Look at this awesome email :-)
		&lt;/div&gt;
	&lt;/body&gt;
&lt;/html&gt;
</pre>
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class XSLTFormatter implements MessageFormatter {

	// Shared instance of the transformer factory
	private static final TransformerFactory transformerFactory = TransformerFactory.newInstance();

	@Override
	public void writeTo(MessageContext messageContext, OMOutputFormat format, OutputStream outputStream,
			boolean preserve) throws AxisFault {

		try {
			Element env = AxiomDOMConvertor.toDOM(messageContext.getEnvelope());
			if (env == null)
				throw new AxisFault("Missing SOAP envelope in message");

			Transformer transformer = getTransformer(messageContext, env);

			NodeList parameters = env.getElementsByTagName("parameters");
			if (parameters.getLength() == 1) {
				parameters = ((Element) parameters.item(0)).getElementsByTagName("parameter");
				for(int i = 0; i < parameters.getLength(); i++) {
					Element p = (Element) parameters.item(i);
					transformer.setParameter(p.getAttribute("name"), p.getTextContent());
				}
			}

			Node child = env.getElementsByTagNameNS(env.getNamespaceURI(), "Body").item(0).getFirstChild();
			while (child != null && !(child instanceof Element)) {
				child = child.getNextSibling();
			};

			if (child == null)
				throw new AxisFault("Missing XML to transform in Body");

			final Source xmlDoc = new DOMSource(child.getOwnerDocument());
			final OutputStreamWriter result = new OutputStreamWriter(outputStream);
			transformer.transform(xmlDoc, new StreamResult(result));
			result.flush();
		} catch (TransformerException | IOException transformationFailure) {
			throw new AxisFault("Error in transformation of XML", transformationFailure);
		}
	}

	@Override
	public String getContentType(MessageContext messageContext, OMOutputFormat format, String soapAction) {
		try {
			Transformer transformer = getTransformer(messageContext);
			return "text/" + transformer.getOutputProperty("method");
		} catch (AxisFault | TransformerConfigurationException e) {
			return null;
		}
	}

	@Override
	public URL getTargetAddress(MessageContext messageContext, OMOutputFormat format, URL targetURL) throws AxisFault {
		return targetURL;
	}

	@Override
	public String formatSOAPAction(MessageContext messageContext, OMOutputFormat format, String soapAction) {
		return null;
	}

	/**
	 * Gets the transformer to use for applying the XLST. Because the transformer is both needed to create the actual
	 * output and get the Content-Type the transformer is only created once and stored in the Message Context.
	 *
	 * @param mc	message context
	 * @param env	DOM representation of the SOAP envelope if available
	 * @return	the transformer
	 * @throws AxisFault when the message context doesn't contain a SOAP envelope or stylesheet in the SOAP header
	 * @throws TransformerConfigurationException when the transformer cannot be created
	 */
	private Transformer getTransformer(MessageContext mc, Element... env) throws AxisFault, TransformerConfigurationException {
		Transformer transformer = (Transformer) mc.getProperty("xsltformatter:transformer");
		if (transformer != null)
			return transformer;

		Element envelope;
		try {
			envelope = env != null && env.length == 1 ? env[0] : AxiomDOMConvertor.toDOM(mc.getEnvelope());
		} catch (TransformerException e) {
			envelope = null;
		}
		if (envelope == null)
			throw new AxisFault("Missing SOAP envelope in message");

		NodeList stylesheet = envelope.getElementsByTagNameNS("http://www.w3.org/1999/XSL/Transform", "stylesheet");
		if (stylesheet.getLength() == 0)
			throw new AxisFault("Missing XSLT stylesheet in header");

		transformer = transformerFactory.newTransformer(new DOMSource(stylesheet.item(0)));

		mc.setProperty("xsltformatter:transformer", transformer);
		return transformer;
	}
}
