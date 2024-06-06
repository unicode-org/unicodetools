package org.unicode.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.FileOutputStream;

public class UCDXMLWriter {

    public static final String NAMESPACE
            = "http://www.unicode.org/ns/2003/ucd/1.0";

    private final TransformerHandler transformerHandler;

    public TransformerHandler getTransformerHandler() {
        return transformerHandler;
    }

    public UCDXMLWriter(FileOutputStream f) throws TransformerConfigurationException {
        TransformerFactory tfactory = TransformerFactory.newInstance();
        SAXTransformerFactory sfactory = (SAXTransformerFactory) tfactory;
        transformerHandler = sfactory.newTransformerHandler ();
        Transformer transformer = transformerHandler.getTransformer ();
        transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "3");
        transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "3");
        transformerHandler.setResult (new StreamResult(f));
    }

    public void startFile() throws SAXException {
        transformerHandler.startDocument ();
        char[] c = "\n".toCharArray ();
        transformerHandler.characters (c, 0, c.length);
        //TODO: JRW change hardcoded 2023 to current year.
        c = " \u00A9 2023 Unicode\u00AE, Inc. ".toCharArray ();
        transformerHandler.comment (c, 0, c.length);
        c = "\n".toCharArray ();
        transformerHandler.characters (c, 0, c.length);
        c = " For terms of use, see http://www.unicode.org/terms_of_use.html ".toCharArray ();
        transformerHandler.comment (c, 0, c.length);
        c = "\n\n\n".toCharArray ();
        transformerHandler.characters (c, 0, c.length);

    }
    public void endFile() throws SAXException {
        transformerHandler.endDocument ();
    }

    public void startElement(String tagName) throws SAXException {
        AttributesImpl attributes = new AttributesImpl ();
        startElement(tagName, attributes);
    }

    public void startElement(String tagName, AttributesImpl attributes) throws SAXException {
        transformerHandler.startElement (NAMESPACE, tagName, tagName, attributes);
    }

    public void addContent(String s) throws SAXException {
        char[] d = s.toCharArray ();
        transformerHandler.characters (d, 0, d.length);
    }

    public void endElement(String tagName) throws SAXException {
        transformerHandler.endElement (NAMESPACE, tagName, tagName);
    }
}


