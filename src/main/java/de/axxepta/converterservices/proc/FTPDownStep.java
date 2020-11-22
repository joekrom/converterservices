package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.utils.FTPUtils;
import de.axxepta.converterservices.utils.IOUtils;
import de.axxepta.converterservices.utils.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class FTPDownStep extends Step {

    FTPDownStep(String name, Object input, Object output, Object additional, boolean stopOnError, String... params) {
        super(name, input, output, additional, stopOnError, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.FTP_DOWN;
    }

    @Override
    Object execAction(final List<String> inputFiles, final String... parameters) throws Exception {
        String server = pipe.getFtpHost();
        String user = pipe.getFtpUser();
        String pwd = pipe.getFtpPwd();
        int port = pipe.getFtpPort();
        boolean secure = pipe.isFtpSecure();
        boolean recursive = false;
        boolean single = false;
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
                    case "recursive":
                        if (parts[1].toLowerCase().equals("true")) {
                            recursive = true;
                        }
                        break;
                    case "single": case "singlefile":
                        if (parts[1].toLowerCase().equals("true")) {
                            single = true;
                        }
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

        List<String> outputFiles = new ArrayList<>();
        for (String inFile : inputFiles) {

            String inputPath = IOUtils.relativePath(inFile, pipe.getInputPath()).replaceAll("\\\\", "/");
            inputPath = inputPath.endsWith("/") ? inputPath.substring(0, inputPath.length() - 1) : inputPath;

            if (single) {

                String outputFile = IOUtils.pathCombine(pipe.getWorkPath(), IOUtils.filenameFromPath(inputPath));
                download(secure, user, pwd, server, port, inputPath, outputFile, timeout);
                outputFiles.add(outputFile);

            } else {

                String[] serverList = list(secure, user, pwd, server, port, inputPath, timeout);

                String clientPath = "";
                if (serverList.length > 1) {
                    clientPath = IOUtils.filenameFromPath(inputPath);
                    IOUtils.safeCreateDirectory(IOUtils.pathCombine(pipe.getWorkPath(), clientPath));
                }

                for (String serverListItem : serverList) {
                    if (serverListItem.startsWith(FTPUtils.FILE_ENTRY)) {
                        String serverFile = serverListItem.substring(FTPUtils.FILE_ENTRY.length());
                        // ToDo: adjust in FTPUtils
                        serverFile = secure ? serverFile : IOUtils.filenameFromPath(serverFile);
                        String outputFile;
                        if (serverList.length == 1) {
                            outputFile = IOUtils.pathCombine(pipe.getWorkPath(), serverFile);
                            serverFile = inputPath;
                        } else {
                            outputFile = IOUtils.pathCombine(pipe.getWorkPath(), clientPath) + File.separator + serverFile;
                            serverFile = IOUtils.pathCombine(inputPath, serverFile).replaceAll("\\\\", "/");
                        }

                        download(secure, user, pwd, server, port, serverFile, outputFile, timeout);
                        outputFiles.add(outputFile);
                    } else if (serverListItem.startsWith(FTPUtils.DIR_ENTRY) && serverListItem.length() > FTPUtils.DIR_ENTRY.length()) {
                        String dirName = serverListItem.substring(FTPUtils.DIR_ENTRY.length());
                        // ToDo: see above
                        dirName = secure ? dirName : IOUtils.filenameFromPath(dirName);
                        if (recursive && !(dirName.equals(".") || dirName.equals(".."))) {
                            recursiveDownload(inputPath + "/" + dirName, clientPath,
                                    secure, user, pwd, server, port, pipe, outputFiles, timeout);
                        }
                    }
                }

            }
        }

        pipe.addGeneratedFiles(outputFiles);
        return outputFiles;
    }

    private void download(boolean secure, String user, String pwd, String server, int port, String inputPath,
                          String outputFile, int timeout) throws Exception
    {
        if (timeout > 0) {
            FTPUtils.download(secure, user, pwd, server, port, inputPath, outputFile, timeout);
        } else {
            FTPUtils.download(secure, user, pwd, server, port, inputPath, outputFile);
        }
    }

    private String[] list(boolean secure, String user, String pwd, String server, int port, String inputPath,
                          int timeout) throws Exception
    {
        return StringUtils.nlListToArray(timeout > 0
                ? FTPUtils.list(secure, user, pwd, server, port, inputPath, timeout)
                : FTPUtils.list(secure, user, pwd, server, port, inputPath)
        );
    }

    private void recursiveDownload(String path, String clientPath, boolean secure, String user, String pwd, String server,
                                   int port, Pipeline pipe, List<String> outputFiles, int timeout) throws Exception
    {
        String serverDir = IOUtils.filenameFromPath(path);
        String clientDir = clientPath + File.separator + serverDir;
        IOUtils.safeCreateDirectory(IOUtils.pathCombine(pipe.getWorkPath(), clientDir));

        String[] serverList = list(secure, user, pwd, server, port, path, timeout);
        for (String serverListItem : serverList) {
            if (serverListItem.startsWith(FTPUtils.FILE_ENTRY)) {
                String serverFile = serverListItem.substring(FTPUtils.FILE_ENTRY.length());
                // ToDo
                serverFile = secure ? serverFile : IOUtils.filenameFromPath(serverFile);
                String outputFile = IOUtils.pathCombine(pipe.getWorkPath(), clientDir) + File.separator + serverFile;
                serverFile = path + "/" + serverFile;

                download(secure, user, pwd, server, port, serverFile, outputFile, timeout);
                outputFiles.add(outputFile);
            } else {
                String dirName = serverListItem.substring(FTPUtils.DIR_ENTRY.length());
                // ToDO
                dirName = secure ? dirName : IOUtils.filenameFromPath(dirName);
                if ( !(dirName.equals(".") || dirName.equals("..")) ) {
                    recursiveDownload(path + "/" + dirName, clientDir,
                            secure, user, pwd, server, port, pipe, outputFiles, timeout);
                }
            }
        }
    }

    @Override
    protected boolean assertParameter(final Parameter paramType, final Object param) {
        return true;
    }
}
