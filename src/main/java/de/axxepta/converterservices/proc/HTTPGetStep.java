package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.utils.HTTPUtils;
import de.axxepta.converterservices.utils.IOUtils;
import de.axxepta.converterservices.utils.StringUtils;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

class HTTPGetStep extends Step {

    HTTPGetStep(String name, Object input, Object output, Object additional, boolean stopOnError, String... params) {
        super(name, input, output, additional, stopOnError, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.HTTP_GET;
    }

    @Override
    Object execAction(final List<String> inputFiles, final String... parameters) throws Exception {

        String server = pipe.getHttpHost();
        String user = pipe.getHttpUser();
        String pwd = pipe.getHttpPwd();
        int port = pipe.getHttpPort();
        boolean secure = pipe.isHttpSecure();
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
                        if (parts[1].toLowerCase().equals("false")) {
                            secure = false;
                            if (port == 0) {
                                port = 80;
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
                    case "content": case "contenttype": case "accept":
                        contentTypeString = parts[1];
                        break;
                }
            }
        }
        if (port == 0) {
            port = 443;
        }

        List<String> downloadedFiles = new ArrayList<>();
        String inputPath = IOUtils.relativePath(inputFiles.get(0), pipe.getInputPath());

        try {
            // ToDo: handle multipart responses, eventually multiple get requests/inputs
            String outputFile = standardOutputFile(pipe);
            List<String> responseFiles;
            if (contentTypeString.equals("")) {
                responseFiles = HTTPUtils.get(secure ? "https" : "http", server, port, inputPath, user, pwd, timeout, outputFile);
            } else {
                responseFiles = HTTPUtils.get(secure ? "https" : "http", server, port, inputPath, user, pwd, timeout, outputFile, contentTypeString);
            }
            downloadedFiles.addAll(responseFiles);
        } catch (SocketTimeoutException ex) {
            pipe.log(String.format("Timeout during HTTP GET to %s", (secure ? "https" : "http" + server + port + inputPath) ));
            if (stopOnError) {
                throw ex;
            }
        } catch (IOException ex) {
            pipe.log(String.format("Error during HTTP GET to %s: %s",
                    (secure ? "https" : "http" + server + port + inputPath), ex.getMessage()));
            throw ex;
        }

        return downloadedFiles;
    }

    @Override
    boolean assertParameter(Parameter paramType, Object param) {
        return true;
    }
}
