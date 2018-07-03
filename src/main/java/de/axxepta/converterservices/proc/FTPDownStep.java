package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.utils.FTPUtils;
import de.axxepta.converterservices.utils.IOUtils;
import de.axxepta.converterservices.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

class FTPDownStep extends Step {

    FTPDownStep(String name, Object input, Object output, Object additional, String... params) {
        super(name, input, output, additional, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.FTP_DOWN;
    }

    @Override
    Object execAction(final Pipeline pipe, final List<String> inputFiles, final String... parameters) throws Exception {
        String server = "";
        String user = "";
        String pwd = "";
        String port = "";
        String base = pipe.getWorkPath();
        String path = "";
        boolean secure = false;
        boolean recursive = false;

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
                    case "recursive":
                        if (parts[1].toLowerCase().equals("true")) {
                            recursive = true;
                        }
                        break;
                }
            }
        }

        List<String> outputFiles = new ArrayList<>();
        for (String inFile : inputFiles) {
            String inputPath = IOUtils.relativePath(inFile, pipe.getInputPath());
            String[] serverList = StringUtils.nlListToArray(
                    FTPUtils.list(secure, user, pwd, server, Integer.valueOf(port), inputPath));
            for (String serverListItem : serverList) {
                if (serverListItem.startsWith("file ")) {
                    String serverFile = serverListItem.substring(5);
                    String outputFile = IOUtils.pathCombine(pipe.getWorkPath(), serverFile);
                    FTPUtils.download(secure, user, pwd, server, Integer.valueOf(port), serverFile, outputFile);
                    outputFiles.add(outputFile);
                }
            }
        }

        pipe.addGeneratedFiles(outputFiles);
        actualOutput = outputFiles;
        return outputFiles;
    }

    @Override
    protected boolean assertParameter(final Parameter paramType, final Object param) {
        return true;
    }
}
