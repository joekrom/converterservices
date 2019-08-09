package de.axxepta.converterservices.utils;


import de.axxepta.converterservices.tools.Saxon;
import de.axxepta.emailwrapper.Mail;
import org.apache.commons.mail.EmailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

public final class ExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandler.class);

    private static final String CONFIG_FILE = "config.xml";

    private static final String MAIL_TAG = "exceptionHandler/mail";
/*    private static final String MAIL_HOST_TAG = "exceptionHandler/mail/host";
    private static final String MAIL_USER_TAG = "exceptionHandler/mail/user";
    private static final String MAIL_PWD_TAG = "exceptionHandler/mail/pwd";
    private static final String MAIL_PORT_TAG = "exceptionHandler/mail/port";
    private static final String MAIL_SENDER_TAG = "exceptionHandler/mail/sender";
    private static final String MAIL_RECEIVER_TAG = "exceptionHandler/mail/receiver";
    private static final String MAIL_SECURE_TAG = "exceptionHandler/mail/secure";
    private static final String MAIL_SUBJECT_TAG = "exceptionHandler/mail/subject";*/
    private static final String MAIL_HOST_TAG = "host";
    private static final String MAIL_USER_TAG = "user";
    private static final String MAIL_PWD_TAG = "pwd";
    private static final String MAIL_PORT_TAG = "port";
    private static final String MAIL_SENDER_TAG = "sender";
    private static final String MAIL_RECEIVER_TAG = "receiver";
    private static final String MAIL_SECURE_TAG = "secure";
    private static final String MAIL_SUBJECT_TAG = "subject";

    private static final String STD_SUBJECT = "CONVERTERSERVICES EXCEPTION";

    private static Map<String, String> config = new HashMap<>();

    private static List<Observer> errorObservers = new ArrayList<>();
    private static List<Observer> exceptionObservers = new ArrayList<>();

    static {
        config.put(MAIL_HOST_TAG, "");
        config.put(MAIL_USER_TAG, "");
        config.put(MAIL_PORT_TAG, "587");
        config.put(MAIL_PWD_TAG, "");
        config.put(MAIL_SECURE_TAG, "");
        config.put(MAIL_SENDER_TAG, "");
        config.put(MAIL_SUBJECT_TAG, STD_SUBJECT);
        config.put(MAIL_RECEIVER_TAG, "");
    }

    private ExceptionHandler() {}

    public static void addErrorObserver(Observer observer) {
        errorObservers.add(observer);
    }

    private static void notifyErrorObservers(Object arg) {
        errorObservers.forEach(e-> e.update(null, arg));
    }

    public static void addExceptionObserver(Observer observer) {
        exceptionObservers.add(observer);
    }

    private static void notifyExceptionObservers(Object arg) {
        exceptionObservers.forEach(e-> e.update(null, arg));
    }

    public static void initDefaultExceptionHandler() {
        String path = IOUtils.pathCombine(IOUtils.jarPath(), CONFIG_FILE);
        if (IOUtils.pathExists(path)) {
            try {
                loadExceptionHandlerConfig(path);
            } catch (Exception ex) {
                LOGGER.error("Error while loading exception handler configuration file: " + ex.getMessage());
            }
        }
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
    }

    private static void loadExceptionHandlerConfig(String path)
            throws SAXException, IOException, ParserConfigurationException, XPathExpressionException
    {
        Document dom = Saxon.loadDOM(path);
        XPathFactory factory = XPathFactory.newInstance();
        XPath xPath = factory.newXPath();
        Node mailNode = (Node) xPath.compile("//" + MAIL_TAG).evaluate(dom, XPathConstants.NODE);
        if (mailNode != null) {
            for (String key : config.keySet()) {
                extractConfigVal(key, mailNode, xPath);
            }
        }
    }

    private static void extractConfigVal(String key, Node mailNode, XPath xPath) throws XPathExpressionException {
        Node node = (Node) xPath.compile("./" + key).evaluate(mailNode, XPathConstants.NODE);
        if (node != null) {
            config.replace(key, node.getTextContent());
        }
    }

    /**
     * Set credentials for an SMTP(S) mail account that is used to send error messages in case of an exception
     * @param host Host name of the SMTP(S) mail server
     * @param user Mail server user name
     * @param pwd Mail server password
     * @param port Mail server port
     * @param sender Sender address
     * @param receiver Receiver mail address
     * @param subject Subject of the error mails to be sent
     */
    public static void setMailCredentials(String host, String user, String pwd, String port, String sender, String receiver, String... subject) {
        config.replace(MAIL_HOST_TAG, host);
        config.replace(MAIL_USER_TAG, user);
        config.replace(MAIL_PWD_TAG, pwd);
        config.replace(MAIL_PORT_TAG, port);
        config.replace(MAIL_SENDER_TAG, sender);
        config.replace(MAIL_RECEIVER_TAG, receiver);
        if (subject.length > 0) {
            config.replace(MAIL_RECEIVER_TAG, subject[0]);
        }
    }

    @Override
    public void uncaughtException(Thread t, Throwable throwable) {
        String stackTrace = getStackTrace(throwable);
        LOGGER.error(stackTrace);
        handle(stackTrace);
        if (throwable instanceof Error) {
            notifyErrorObservers(stackTrace);
            System.exit(1);
        }
    }

    public static String getStackTrace(Throwable throwable) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        throwable.printStackTrace(printWriter);
        return result.toString();
    }

    /**
     * Invoke error handling with a predefined message
     * @param message Error message
     */
    public static void handle(String message) {
        sendMail(message);
        notifyExceptionObservers(message);
    }

    private static void sendMail(String message) {
        if (!config.get(MAIL_HOST_TAG).equals("") &&
                !config.get(MAIL_USER_TAG).equals("") &&
                !config.get(MAIL_PWD_TAG).equals("") &&
                !config.get(MAIL_SENDER_TAG).equals("") &&
                !config.get(MAIL_RECEIVER_TAG).equals("")
        ) {
            int port;
            try {
                port = Integer.parseInt(config.get(MAIL_PORT_TAG));
            } catch (NumberFormatException ne) {
                port = 587;
            }
            try {
                Mail.sendMail(
                        config.get(MAIL_SECURE_TAG).toLowerCase().equals("true"),
                        config.get(MAIL_HOST_TAG),
                        port,
                        config.get(MAIL_USER_TAG),
                        config.get(MAIL_PWD_TAG),
                        config.get(MAIL_SENDER_TAG),
                        config.get(MAIL_RECEIVER_TAG),
                        config.get(MAIL_SUBJECT_TAG).equals("") ? STD_SUBJECT : config.get(MAIL_SUBJECT_TAG),
                        message
                );
            } catch (EmailException ex) {
                LOGGER.warn(ex.getMessage());
            }
        }
    }
}
