package de.axxepta.converterservices.auth;

import de.axxepta.converterservices.tools.Saxon;
import de.axxepta.converterservices.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class CredentialsProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialsProvider.class);

    private static final String USER = "ONKEL_HOTTE";
    private static final String PWD = "Märchenstunde";

    private static final String AUTH_FILE = "credentials.xml";

    private static final String CREDENTIALS_TAG = "credentials";
    private static final String USER_TAG = "user";
    private static final String PWD_TAG = "pwd";

    private CredentialsProvider() {}

    static  List<AuthenticationDetails> getCredentials() {
        final List<AuthenticationDetails> credentialsList = new ArrayList<>();
        if (IOUtils.pathExists(AUTH_FILE)) {
            try {
                loadCredentialFile(credentialsList);
            } catch (Exception ex) {
                LOGGER.error("Error while loading Basic Auth credentials file: " + ex.getMessage());
            }
        }
        if (credentialsList.size() == 0) {
            credentialsList.add(new AuthenticationDetails(USER, PWD));
        }
        return credentialsList;
    }

    private static void loadCredentialFile(final List<AuthenticationDetails> credentialsList)
            throws SAXException, IOException, ParserConfigurationException, XPathExpressionException
    {
        Document dom = Saxon.loadDOM(AUTH_FILE);
        XPathFactory factory = XPathFactory.newInstance();
        XPath xPath = factory.newXPath();
        NodeList credentialNodes = (NodeList) xPath.compile("//" + CREDENTIALS_TAG).evaluate(dom, XPathConstants.NODESET);
        for (int c = 0; c < credentialNodes.getLength(); c++) {
            Node userNode = (Node) xPath.compile("./" + USER_TAG).evaluate(credentialNodes.item(c), XPathConstants.NODE);
            Node pwdNode = (Node) xPath.compile("./" + PWD_TAG).evaluate(credentialNodes.item(c), XPathConstants.NODE);
            if (userNode != null && pwdNode != null && !userNode.getTextContent().equals("") && !pwdNode.getTextContent().equals("")) {
                credentialsList.add(new AuthenticationDetails(userNode.getTextContent(), pwdNode.getTextContent()));
            }
        }
    }

}