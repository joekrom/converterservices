package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.utils.HTTPUtils;
import de.axxepta.converterservices.utils.IOUtils;
import de.axxepta.converterservices.utils.StringUtils;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class HTTPPostStep extends Step {

    HTTPPostStep(String name, Object input, Object output, Object additional, boolean stopOnError, String... params) {
        super(name, input, output, additional, stopOnError, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.HTTP_POST;
    }

    @Override
    Object execAction(final Pipeline pipe, final List<String> inputFiles, final String... parameters)
            throws Exception
    {
        String server = "";
        String user = "";
        String pwd = "";
        int port = 80;
        String path = "";
        boolean secure = false;
        String contentTypeString = "";
        int timeout = 1200;

        for (String parameter : parameters) {
            String[] parts = parameter.split(" *= *");
            if (parts.length > 1) {
                switch (parts[0].toLowerCase()) {
                    case "server": case "host":
                        server = parts[1];
                        break;
                    case "user":
                        user = parts[1];
                        break;
                    case "pwd": case "password":
                        pwd = parts[1];
                        break;
                    case "secure": case "ssl":
                        if (parts[1].toLowerCase().equals("true")) {
                            secure = true;
                            if (port == 80) {
                                port = 443;
                            }
                        }
                        break;
                    case "port":
                        if (StringUtils.isInt(parts[1])) {
                            port = Integer.valueOf(parts[1]);
                        }
                        break;
                    case "timeout":
                        if (StringUtils.isInt(parts[1])) {
                            timeout = Integer.valueOf(parts[1]);
                        }
                        break;
                    case "path":
                        path = parts[1];
                        break;
                    case "content": case "contenttype":
                        contentTypeString = parts[1];
                        break;
                }
            }
        }

        List<String> uploadFiles = IOUtils.collectFiles(inputFiles);
        List<String> uploadedFiles = new ArrayList<>();

        for (String file : uploadFiles) {
            try {
                if (HTTPUtils.contentTypeIsTextType(contentTypeString) ||
                        HTTPUtils.fileTypeIsTextType(IOUtils.getFileExtension(file))) {
                    ContentType currentContent = determineContentType(contentTypeString, IOUtils.getFileExtension(file));
                    HTTPUtils.postTextTypeFile(secure ? "https" : "http", server, port, path, user, pwd, timeout, file, currentContent);
                    uploadedFiles.add(file);
                }
            } catch (IOException ex) {
                pipe.log(String.format("Error during HTTP POST file transfer of file %s: %s", file, ex.getMessage()));
            }
        }

        return uploadedFiles;
    }

    private ContentType determineContentType(final String contentTypeString, final String fileExtension) {
        switch (contentTypeString.toLowerCase()) {
            case "application/javascript": case "text/plain": case "text/css": case "text/csv":
                return ContentType.TEXT_PLAIN;
            case "application/json":
                return ContentType.APPLICATION_JSON;
            case "application/xml": case "application/rdf+xml": case "text/xml":
                return ContentType.APPLICATION_XML;
            case "application/atom+xml":
                return ContentType.APPLICATION_ATOM_XML;
            case "text/html":
                return ContentType.TEXT_HTML;
            case "application/svg+xml":
                return ContentType.APPLICATION_SVG_XML;
            case "image/jpg": case "image/png": case "image/gif": case "image/tiff":
            case "application/zip": case "application/pdf":
                return ContentType.APPLICATION_OCTET_STREAM;
        }
        switch (fileExtension.toLowerCase()) {
            case "txt": case "css": case "md5":
                return ContentType.TEXT_PLAIN;
            case "json":
                return ContentType.APPLICATION_JSON;
            case "xml":
                return ContentType.APPLICATION_XML;
            case "asf":
                return ContentType.APPLICATION_ATOM_XML;
            case "htm":
                return ContentType.TEXT_HTML;
        }
        return ContentType.APPLICATION_OCTET_STREAM;
    }

    @Override
    protected boolean assertParameter(final Parameter paramType, final Object param) {
        return true;
    }
}
