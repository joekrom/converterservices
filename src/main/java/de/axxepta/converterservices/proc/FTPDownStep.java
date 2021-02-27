package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.security.RSACryptor;
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
        FTPUtils.Protocol protocol = FTPUtils.Protocol.SFTP;
        boolean recursive = false;
        boolean single = false;
        int timeout = -1;

        for (String parameter : parameters) {
            String[] parts = parameter.split(" *= *");
            if (parts.length > 1) {
                String val = parts[1].toLowerCase();
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
                    case "secure":
                        if (val.equals("false")) {
                            secure = false;
                            if (port == 0) {
                                port = 21;
                            }
                            protocol = FTPUtils.Protocol.FTP;
                        } else {
                            secure = true;
                            if (port == 0) {
                                port = 22;
                            }
                            protocol = FTPUtils.Protocol.SFTP;
                        }
                        break;
                    case "sftp":
                        if (val.equals("true")) {
                            protocol = FTPUtils.Protocol.SFTP;
                            if (port == 0) {
                                port = 22;
                            }
                        }
                        break;
                    case "ftps":
                        if (val.equals("true")) {
                            secure = true;
                            if (port == 0) {
                                port = 21;
                            }
                            protocol = FTPUtils.Protocol.FTPS;
                        }
                        break;
                    case "protocol":
                        if (val.equals("ftp")) {
                            secure = false;
                            protocol = FTPUtils.Protocol.FTP;
                            if (port == 0) {
                                port = 21;
                            }
                            break;
                        }
                        secure = true;
                        if (val.equals("ftps") || val.contains("ssl") || val.contains("tls")) {
                            protocol = FTPUtils.Protocol.FTPS;
                            if (port == 0) {
                                port = 21;
                            }
                        }
                        if (val.equals("sftp") || val.contains("ssh")) {
                            protocol = FTPUtils.Protocol.SFTP;
                            if (port == 0) {
                                port = 22;
                            }
                        }
                        break;
                    case "port":
                        if (StringUtils.isInt(parts[1])) {
                            port = Integer.parseInt(parts[1]);
                        }
                        break;
                    case "recursive":
                        if (val.equals("true")) {
                            recursive = true;
                        }
                        break;
                    case "single": case "singlefile":
                        if (val.equals("true")) {
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

        if (!StringUtils.isNoStringOrEmpty(pwd)) {
            pwd = RSACryptor.decrypt(pwd);
        }

        List<String> outputFiles = new ArrayList<>();
        for (String inFile : inputFiles) {

            String inputPath = IOUtils.relativePath(inFile, pipe.getInputPath()).replaceAll("\\\\", "/");
            inputPath = inputPath.endsWith("/") ? inputPath.substring(0, inputPath.length() - 1) : inputPath;

            if (single) {

                String outputFile = IOUtils.pathCombine(pipe.getWorkPath(), IOUtils.filenameFromPath(inputPath));
                download(secure, user, pwd, server, port, inputPath, outputFile, protocol, timeout);
                outputFiles.add(outputFile);

            } else {

                String[] serverList = list(secure, user, pwd, server, port, inputPath, protocol, timeout);

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

                        download(secure, user, pwd, server, port, serverFile, outputFile, protocol, timeout);
                        outputFiles.add(outputFile);
                    } else if (serverListItem.startsWith(FTPUtils.DIR_ENTRY) && serverListItem.length() > FTPUtils.DIR_ENTRY.length()) {
                        String dirName = serverListItem.substring(FTPUtils.DIR_ENTRY.length());
                        // ToDo: see above
                        dirName = secure ? dirName : IOUtils.filenameFromPath(dirName);
                        if (recursive && !(dirName.equals(".") || dirName.equals(".."))) {
                            recursiveDownload(inputPath + "/" + dirName, clientPath,
                                    secure, user, pwd, server, port, pipe, outputFiles, protocol, timeout);
                        }
                    }
                }

            }
        }

        pipe.addGeneratedFiles(outputFiles);
        return outputFiles;
    }

    private void download(boolean secure, String user, String pwd, String server, int port, String inputPath,
                          String outputFile, FTPUtils.Protocol protocol, int timeout) throws Exception
    {
        if (timeout > 0) {
            FTPUtils.download(secure, user, pwd, server, port, inputPath, outputFile, protocol, timeout);
        } else {
            FTPUtils.download(secure, user, pwd, server, port, inputPath, outputFile, protocol);
        }
    }

    private String[] list(boolean secure, String user, String pwd, String server, int port, String inputPath,
                          FTPUtils.Protocol protocol, int timeout) throws Exception
    {
        return StringUtils.nlListToArray(timeout > 0
                ? FTPUtils.list(secure, user, pwd, server, port, inputPath, protocol, timeout)
                : FTPUtils.list(secure, user, pwd, server, port, inputPath, protocol)
        );
    }

    private void recursiveDownload(String path, String clientPath, boolean secure, String user, String pwd, String server,
                                   int port, Pipeline pipe, List<String> outputFiles, FTPUtils.Protocol protocol, int timeout) throws Exception
    {
        String serverDir = IOUtils.filenameFromPath(path);
        String clientDir = clientPath + File.separator + serverDir;
        IOUtils.safeCreateDirectory(IOUtils.pathCombine(pipe.getWorkPath(), clientDir));

        String[] serverList = list(secure, user, pwd, server, port, path, protocol, timeout);
        for (String serverListItem : serverList) {
            if (serverListItem.startsWith(FTPUtils.FILE_ENTRY)) {
                String serverFile = serverListItem.substring(FTPUtils.FILE_ENTRY.length());
                // ToDo
                serverFile = secure ? serverFile : IOUtils.filenameFromPath(serverFile);
                String outputFile = IOUtils.pathCombine(pipe.getWorkPath(), clientDir) + File.separator + serverFile;
                serverFile = path + "/" + serverFile;

                download(secure, user, pwd, server, port, serverFile, outputFile, protocol, timeout);
                outputFiles.add(outputFile);
            } else {
                String dirName = serverListItem.substring(FTPUtils.DIR_ENTRY.length());
                // ToDO
                dirName = secure ? dirName : IOUtils.filenameFromPath(dirName);
                if ( !(dirName.equals(".") || dirName.equals("..")) ) {
                    recursiveDownload(path + "/" + dirName, clientDir,
                            secure, user, pwd, server, port, pipe, outputFiles, protocol, timeout);
                }
            }
        }
    }

    @Override
    protected boolean assertParameter(final Parameter paramType, final Object param) {
        return true;
    }
}
