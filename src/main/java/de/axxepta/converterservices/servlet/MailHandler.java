package de.axxepta.converterservices.servlet;

import de.axxepta.converterservices.utils.IOUtils;
import de.axxepta.emailwrapper.Mail;
import org.apache.commons.mail.EmailException;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static de.axxepta.converterservices.servlet.ServletUtils.*;

public class MailHandler extends RequestHandler {

    public MailHandler(Request request, Response response, String path, Map<String, String> formFields, Map<String, List<String>> files) {
        super(request, response, path, formFields, files);
    }

    @Override
    public Object processMulti() {
        List<String> files = this.files.getOrDefault(FILE_PART, new ArrayList<>());
        files = files.stream().map(e -> IOUtils.pathCombine(path, e)).collect(Collectors.toList());

        String host = formFields.getOrDefault(PARAM_HOST, "");
        int port;
        try {
            port = Integer.parseInt(formFields.getOrDefault(PARAM_PORT, "587"));
        } catch (NumberFormatException ne) {
            port = 587;
        }
        String user = formFields.getOrDefault(PARAM_USER, "");
        String pwd = formFields.getOrDefault(PARAM_PWD, "");
        String sender = formFields.getOrDefault(PARAM_SENDER, "");
        String receiver = formFields.getOrDefault(PARAM_RECEIVER, "");
        if (host.equals("") || user.equals("") || pwd.equals("") || sender.equals("") || receiver.equals("")) {
            response.status(422);
            return wrapResponse("Missing parameter (HOST|USER|PWD|SENDER|RECEIVER)");
        }
        boolean secure = formFields.containsKey(PARAM_SECURE);
        String subject = formFields.getOrDefault(PARAM_SUBJECT, "--");
        String content = formFields.getOrDefault(PARAM_CONTENT, "");
        boolean htmlMail = formFields.containsKey(PARAM_HTML);
        boolean embeddedImages = formFields.containsKey(PARAM_IMAGES);
        boolean attachments = formFields.containsKey(PARAM_ATTACHMENTS);
        String imgBaseURL = formFields.getOrDefault(PARAM_BASE, "");

        try {
            if (htmlMail) {
                if (embeddedImages) {
                    return Mail.sendImageHTMLMail(secure, host, port, user, pwd, sender, receiver, subject, content, content, imgBaseURL, true);
                } else {
                    return Mail.sendHTMLMail(secure, host, port, user, pwd, sender, receiver, subject, content, content, true);
                }
            } else {
                if (attachments) {
                    return Mail.sendAttachmentMail(secure, host, port, user, pwd, sender, receiver, subject, content, files, true);
                } else {
                    return Mail.sendMail(secure, host, port, user, pwd, sender, receiver, subject, content, true);
                }
            }
        } catch (EmailException ex) {
            response.status(500);
            LOGGER.error("Error while sending mail:", ex);
            return "<response>" + ex.getMessage() + "</response>";
        }
    }

    @Override
    public Object processSingle(boolean async) {
        response.status(400);
        return "Not implemented";
    }
}
