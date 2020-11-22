package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.utils.FTPUtils;
import de.axxepta.converterservices.utils.IOUtils;
import de.axxepta.converterservices.utils.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class FTPUpStep extends Step {

    FTPUpStep(String name, Object input, Object output, Object additional, boolean stopOnError, String... params) {
        super(name, input, output, additional, stopOnError, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.FTP_UP;
    }

    @Override
    Object execAction(final List<String> inputFiles, final String... parameters)
            throws Exception
    {
        String server = pipe.getFtpHost();
        String user = pipe.getFtpUser();
        String pwd = pipe.getFtpPwd();
        int port = pipe.getFtpPort();
        String base = pipe.getWorkPath();
        String path = "";
        boolean secure = pipe.isFtpSecure();
        int timeout = -1;

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
                                port = 21;
                            }
                        }
                        break;
                    case "port":
                        if (StringUtils.isInt(parts[1])) {
                            port = Integer.parseInt(parts[1]);
                        }
                        break;
                    case "base": case "basepath":
                        if (parts[1].toLowerCase().equals("input")) {
                            base = pipe.getInputPath();
                        } else if (!parts[1].toLowerCase().equals("work")) {
                            base = parts[1];
                        }
                        break;
                    case "path":
                        path = parts[1];
                        break;
                    case "timeout":
                        if (StringUtils.isInt(parts[1])) {
                            timeout = Integer.parseInt(parts[1]);
                        }
                        break;
                }
            }
        }
        if (port == 0) {
            port = 22;
        }

        List<String> uploadFiles = IOUtils.collectFiles(inputFiles, pipe::log);
        List<String> uploadedFiles = new ArrayList<>();

        for (String file : uploadFiles) {
            try {
                if (timeout > 0) {
                    FTPUtils.upload(secure, user, pwd, server, port, path, base, file, timeout);
                } else {
                    FTPUtils.upload(secure, user, pwd, server, port, path, base, file);
                }
                uploadedFiles.add(file);
            } catch (IOException ex) {
                pipe.log(String.format("Error during FTP file transfer of file %s: %s", file, ex.getMessage()));
            }
        }

        return uploadedFiles;
    }

    @Override
    protected boolean assertParameter(final Parameter paramType, final Object param) {
        return true;
    }

}
