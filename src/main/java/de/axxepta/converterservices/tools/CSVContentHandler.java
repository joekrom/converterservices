package de.axxepta.converterservices.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

class CSVContentHandler extends DefaultHandler {

    private static Logger LOGGER = LoggerFactory.getLogger(CSVContentHandler.class);

    private ByteArrayOutputStream builder = new ByteArrayOutputStream();
    private final String row;
    private final String column;
    private final String delimiter;
    private boolean inRowElement = false;
    private boolean rowStarted = false;
    private boolean inColumnElement = false;

    CSVContentHandler(final String row, final String column, final String delimiter) {
        this.row = row;
        this.column = column;
        this.delimiter = delimiter;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equals(row)) {
            inRowElement = true;
            rowStarted = true;
        }
        if (qName.equals(column) && inRowElement) {
            if (rowStarted) {
                rowStarted = false;
            } else {
                try {
                    builder.write(delimiter.getBytes(StandardCharsets.ISO_8859_1));
                } catch (IOException ex) {
                    LOGGER.error("Error converting XML to CSV: ", ex);
                }
            }
            inColumnElement = true;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equals(row)) {
            builder.write((byte) '\r');
            builder.write((byte) '\n');
            rowStarted = false;
            inRowElement = false;
        }
        if (qName.equals(column)) {
            inColumnElement = false;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (inColumnElement) {
            try {
                builder.write(new String(Arrays.copyOfRange(ch, start, start + length)).
                        getBytes(StandardCharsets.ISO_8859_1));
            } catch (IOException ex) {
                LOGGER.error("Error converting XML to CSV: ", ex);
            }
        }
    }

    ByteArrayOutputStream getOutputStream() {
        return builder;
    }


}
