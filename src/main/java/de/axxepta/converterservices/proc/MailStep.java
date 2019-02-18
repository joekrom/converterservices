package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.tools.Saxon;
import de.axxepta.converterservices.utils.IOUtils;
import de.axxepta.converterservices.utils.StringUtils;
import de.axxepta.emailwrapper.Mail;
import org.apache.commons.mail.EmailException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;
import java.util.MissingResourceException;

class MailStep extends Step {


    MailStep(String name, Object input, Object output, Object additional, boolean stopOnError, String... params) {
        super(name, input, output, additional, stopOnError, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.MAIL;
    }

    @Override
    Object execAction(final Pipeline pipe, final List<String> inputFiles, final String... parameters) throws Exception {
        String host = "";
        String user = "";
        String pwd = "";
        int port = 587;
        String sender = "";
        String receiver = "";
        String subject = "";
        String content = "";
        boolean secure = true;
        boolean htmlMail = false;
        boolean embeddedImages = false;
        boolean attachments = false;
        String imgBaseURL = "";

        for (String parameter : parameters) {
            String[] parts = parameter.split(" *= *");
            if (parts.length > 1) {
                switch (parts[0].toLowerCase()) {
                    case "server": case "host":
                        host = parts[1];
                        break;
                    case "user":
                        user = parts[1];
                        break;
                    case "pwd": case "password":
                        pwd = parts[1];
                        break;
                    case "secure": case "ssltls": case "ssl":
                        if (parts[1].toLowerCase().equals("false")) {
                            secure = true;
                        }
                        break;
                    case "port":
                        if (StringUtils.isInt(parts[1])) {
                            port = Integer.valueOf(parts[1]);
                        }
                        break;
                    case "sender": case "from":
                        sender = parts[1];
                        break;
                    case "receiver": case "recipient": case "to":
                        receiver = parts[1];
                        break;
                    case "basepath": case "path": case "imgbasepath": case "imagebasepath":
                        imgBaseURL = parts[1];
                        break;
                    case "html": case "htmlmail":
                        if (parts[1].toLowerCase().equals("true")) {
                            htmlMail = true;
                        }
                        break;
                    case "img": case "images": case "embedimages": case "embeddedimages": case "imageembed": case "imagesembedded":
                        if (parts[1].toLowerCase().equals("true")) {
                            embeddedImages = true;
                        }
                        break;
                    case "attachment": case "attachments": case "att": case "add":
                        if (parts[1].toLowerCase().equals("true")) {
                            attachments = true;
                        }
                        break;
                }
            }
        }

        String sendFile = inputFiles.get(0);
        content = IOUtils.loadStringFromFile(sendFile);
        String outputFile = standardOutputFile(pipe);
        String outputMsg = "";

        try {
            if (host.equals("") || user.equals("") || pwd.equals("") || sender.equals("") || receiver.equals("")) {
                throw new IllegalArgumentException("Missing parameter (HOST|USER|PWD|SENDER|RECEIVER)");
            }
            if (htmlMail) {
                String textContent;
                try {
                    Document doc = Saxon.loadDOM(sendFile);
                    textContent = doc.getTextContent();
                } catch (ParserConfigurationException|SAXException|IOException xx) {
                    textContent = content;
                }
                if (embeddedImages) {
                    outputMsg = Mail.sendImageHTMLMail(secure, host, port, user, pwd, sender, receiver, subject,
                            content, textContent, imgBaseURL, true);
                } else {
                    outputMsg = Mail.sendHTMLMail(secure, host, port, user, pwd, sender, receiver, subject,
                            content, content, true);
                }
            } else {
                if (attachments) {
                    List<String> attachedFiles = resolveInput(additional, pipe, true);
                    outputMsg = Mail.sendAttachmentMail(secure, host, port, user, pwd, sender, receiver, subject,
                            content, attachedFiles, true);
                } else {
                    outputMsg = Mail.sendMail(secure, host, port, user, pwd, sender, receiver, subject,
                            content, true);
                }
            }
        } catch (IllegalArgumentException|EmailException ex) {
            String errMsg = String.format("Error during sending mail to %s: %s", receiver, ex.getMessage());
            pipe.log(errMsg);
            outputMsg = "<failed>" + errMsg + "</failed>";
            if (stopOnError)
                throw ex;
        } finally {
            IOUtils.saveStringToFile(outputMsg, outputFile);
        }

        return singleFileList(outputFile);
    }

    @Override
    boolean assertParameter(final Parameter paramType, final Object param) {
        return true;
    }
}
