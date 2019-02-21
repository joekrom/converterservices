package de.axxepta.converterservices.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;

public class DocTypeExtractionHandler extends DefaultHandler2 {

    private static Logger LOGGER = LoggerFactory.getLogger(DocTypeExtractionHandler.class);

    String[] getDocType() {
        return docType;
    }

    private String[] docType = new String[3];

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        throw new SAXException();
    }

    @Override
    public void startDTD(String name, String publicId, String systemId) throws SAXException {
        docType[0] = name;
        docType[1] = publicId;
        docType[2] = systemId;
        throw new SAXException();
    }
}
