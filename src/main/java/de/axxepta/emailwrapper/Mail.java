package de.axxepta.emailwrapper;

import org.apache.commons.mail.*;
import org.apache.commons.mail.resolver.DataSourceUrlResolver;

import javax.activation.FileDataSource;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Wrapper utility class for Apache commons mail (version 1.5), provides static functions for sending mails, e.g. for use in XQuery.
 * @version 0.1.1
 */
public class Mail {


    /**
     * Send text mail
     * @param sslTls    Use secure transport layer
     * @param host      SMTP host
     * @param port      SMTP/SSMTP port
     * @param user      User name
     * @param pwd       Password
     * @param from      Sender address
     * @param to        Recipient(s) address(es), multiple can be provided separated by comma or semicolon
     * @param subject   Subject
     * @param msg       Message
     * @param reportError Set optional true value if exceptions should be thrown instead of returning a <i>failed</i> xml element with message
     * @return success or failed xml element, latter containing error message
     */
    public static String sendMail(boolean sslTls, String host, int port, String user, String pwd, String from,
                                  String to, String subject, String msg, boolean... reportError) throws EmailException {

        Email email = new SimpleEmail();
        email.setHostName(host);
        email.setSmtpPort(port);
        email.setAuthenticator(new DefaultAuthenticator(user, pwd));
        email.setSSLOnConnect(sslTls);
        email.setStartTLSEnabled(sslTls);
        try {
            email.setFrom(from);
            email.setSubject(subject);
            email.setMsg(msg);
            for (String recipient : to.split(";|,")) {
                email.addTo(recipient);
            }
            email.send();
        } catch (EmailException ex) {
            if (reportError.length > 0 && reportError[0]) {
                throw ex;
            } else {
                ex.printStackTrace();
                return "<failed>" + ex.getMessage() + "</failed>";
            }
        }
        return "<success/>";
    }

    /**
     * Send HTML mail
     * @param sslTls    Use secure transport layer
     * @param host      SMTP host
     * @param port      SMTP/SSMTP port
     * @param user      User name
     * @param pwd       Password
     * @param from      Sender address
     * @param to        Recipient(s) address(es), multiple can be provided separated by comma or semicolon
     * @param subject   Subject
     * @param msg       Message
     * @param msgText   Alternative text message
     * @param reportError Set optional true value if exceptions should be thrown instead of returning a <i>failed</i> xml element with message
     * @return success or failed xml element, latter containing error message
     */
    public static String sendHTMLMail(boolean sslTls, String host, int port, String user, String pwd, String from,
                                      String to, String subject, String msg, String msgText, boolean... reportError)
            throws EmailException
    {
        HtmlEmail email = new HtmlEmail();
        email.setHostName(host);
        email.setSmtpPort(port);
        email.setAuthenticator(new DefaultAuthenticator(user, pwd));
        email.setSSLOnConnect(sslTls);
        email.setStartTLSEnabled(sslTls);
        try {
            email.setFrom(from);
            email.setSubject(subject);
            email.setHtmlMsg(msg);
            email.setTextMsg(msgText);
            for (String recipient : to.split(";|,")) {
                email.addTo(recipient);
            }
            email.send();
        } catch (EmailException ex) {
            if (reportError.length > 0 && reportError[0]) {
                throw ex;
            } else {
                ex.printStackTrace();
                return "<failed>" + ex.getMessage() + "</failed>";
            }
        }
        return "<success/>";
    }


    /**
     * Send HTML mail
     * @param sslTls    Use secure transport layer
     * @param host      SMTP host
     * @param port      SMTP/SSMTP port
     * @param user      User name
     * @param pwd       Password
     * @param from      Sender address
     * @param to        Recipient(s) address(es), multiple can be provided separated by comma or semicolon
     * @param subject   Subject
     * @param msg       Message
     * @param msgText   Alternative text message
     * @param attachments   List of files to be attached
     * @param reportError Set optional true value if exceptions should be thrown instead of returning a <i>failed</i> xml element with message
     * @return success or failed xml element, latter containing error message
     */
    public static String sendHTMLAttachmentMail(boolean sslTls, String host, int port, String user, String pwd, String from,
                                      String to, String subject, String msg, String msgText, List<String> attachments, boolean... reportError)
            throws EmailException
    {
        HtmlEmail email = new HtmlEmail();
        email.setHostName(host);
        email.setSmtpPort(port);
        email.setAuthenticator(new DefaultAuthenticator(user, pwd));
        email.setSSLOnConnect(sslTls);
        email.setStartTLSEnabled(sslTls);
        try {
            email.setFrom(from);
            email.setSubject(subject);
            email.setHtmlMsg(msg);
            email.setTextMsg(msgText);
            for (String recipient : to.split(";|,")) {
                email.addTo(recipient);
            }
            int i = 0;
            for (String path : attachments) {
                String[] fileName = path.split("/|\\\\");
                email.attach(new FileDataSource(path), fileName[fileName.length - 1], "Attachment " + ++i);
                //File file = new File(path);
                //email.embed(file.toURI().toURL(), file.getName());
            }
            email.send();
        } catch (EmailException ex) {
            if (reportError.length > 0 && reportError[0]) {
                throw ex;
            } else {
                ex.printStackTrace();
                return "<failed>" + ex.getMessage() + "</failed>";
            }
        }
        return "<success/>";
    }

    /**
     * Send HTML mail, embed images referenced by img element in body
     * @param sslTls    Use secure transport layer
     * @param host      SMTP host
     * @param port      SMTP/SSMTP port
     * @param user      User name
     * @param pwd       Password
     * @param from      Sender address
     * @param to        Recipient(s) address(es), multiple can be provided separated by comma or semicolon
     * @param subject   Subject
     * @param msg       Message
     * @param msgText   Alternative text message
     * @param baseUrl   Base URL for images to be embedded, provided with relative path
     * @param reportError Set optional true value if exceptions should be thrown instead of returning a <i>failed</i> xml element with message
     * @return success or failed xml element, latter containing error message
     */
    public static String sendImageHTMLMail(boolean sslTls, String host, int port, String user, String pwd, String from,
                                           String to, String subject, String msg, String msgText, String baseUrl,
                                           boolean... reportError)
            throws EmailException
    {
        URL url;
        try {
            url = new URL(baseUrl);
        } catch (MalformedURLException ue) {
            return "<failed>" + ue.getMessage() + "</failed>";
        }

        ImageHtmlEmail email = new ImageHtmlEmail();
        email.setDataSourceResolver(new DataSourceUrlResolver(url));
        email.setHostName(host);
        email.setSmtpPort(port);
        email.setAuthenticator(new DefaultAuthenticator(user, pwd));
        email.setSSLOnConnect(sslTls);
        email.setStartTLSEnabled(sslTls);
        try {
            email.setFrom(from);
            email.setSubject(subject);
            email.setHtmlMsg(msg);
            email.setTextMsg(msgText);
            for (String recipient : to.split(";|,")) {
                email.addTo(recipient);
            }
            email.send();
        } catch (EmailException ex) {
            if (reportError.length > 0 && reportError[0]) {
                throw ex;
            } else {
                ex.printStackTrace();
                return "<failed>" + ex.getMessage() + "</failed>";
            }
        }
        return "<success/>";
    }


    /**
     * Send mail, attach files
     * @param sslTls    Use secure transport layer
     * @param host      SMTP host
     * @param port      SMTP/SSMTP port
     * @param user      User name
     * @param pwd       Password
     * @param from      Sender address
     * @param to        Recipient(s) address(es), multiple can be provided separated by comma or semicolon
     * @param subject   Subject
     * @param msg       Message
     * @param attachments   List of files to be attached
     * @param reportError Set optional true value if exceptions should be thrown instead of returning a <i>failed</i> xml element with message
     * @return success or failed xml element, latter containing error message **/
    public static String sendAttachmentMail(boolean sslTls, String host, int port, String user, String pwd, String from,
                                            String to, String subject, String msg, List<String> attachments,
                                            boolean... reportError)
            throws EmailException
    {
        MultiPartEmail email = new MultiPartEmail();
        email.setHostName(host);
        email.setSmtpPort(port);
        email.setAuthenticator(new DefaultAuthenticator(user, pwd));
        email.setSSLOnConnect(sslTls);
        email.setStartTLSEnabled(sslTls);
        try {
            email.setFrom(from);
            email.setSubject(subject);
            email.setMsg(msg);
            for (String recipient : to.split(";|,")) {
                email.addTo(recipient);
            }
            int i = 0;
            for (String path : attachments) {
                EmailAttachment attachment = new EmailAttachment();
                attachment.setPath(path);
                attachment.setDisposition(EmailAttachment.ATTACHMENT);
                attachment.setDescription("Attachment " + ++i);
                String[] fileName = path.split("/|\\\\");
                attachment.setName(fileName[fileName.length - 1]);
                email.attach(attachment);
            }
            email.send();
        } catch (EmailException ex) {
            if (reportError.length > 0 && reportError[0]) {
                throw ex;
            } else {
                ex.printStackTrace();
                return "<failed>" + ex.getMessage() + "</failed>";
            }
        }
        return "<success/>";
    }

}

