package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.utils.FTPUtils;
import de.axxepta.converterservices.utils.IOUtils;
import de.axxepta.converterservices.utils.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class FTPUpStep extends Step {

    FTPUpStep(Object input, Object output, Object additional, String... params) {
        super(input, output, additional, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.FTP_UP;
    }

    @Override
    Object execAction(final Pipeline pipe, final List<String> inputFiles, final Object additionalInput, final String... parameters)
            throws Exception
    {
        String server = "";
        String user = "";
        String pwd = "";
        String port = "";
        String base = pipe.getWorkPath();
        String path = "";
        boolean secure = false;

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
                            if (port.equals("")) {
                                port = "22";
                            }
                        }
                        break;
                    case "port":
                        if (StringUtils.isInt(parts[1])) {
                            port = parts[1];
                        }
                        break;
                    case "base": case "basepath":
                        if (parts[1].toLowerCase().startsWith("in")) {
                            base = pipe.getInputPath();
                        } else if (!parts[1].toLowerCase().startsWith("work")) {
                            base = parts[1];
                        }
                        break;
                    case "path":
                        path = parts[1];
                        break;
                }
            }
        }

        List<String> uploadFiles = IOUtils.collectFiles(inputFiles);
        List<String> uploadedFiles = new ArrayList<>();

        for (String file : uploadFiles) {
            try {
                FTPUtils.upload(secure, user, pwd, server, port, path, base, file);
                uploadedFiles.add(file);
            } catch (IOException ex) {
                pipe.log(String.format("Error during FTP file transfer of file %s: %s", file, ex.getMessage()));
            }
        }

        actualOutput = uploadedFiles;
        return uploadedFiles;
    }

    @Override
    protected boolean assertParameter(final Parameter paramType, final Object param) {
        return true;
    }

}
