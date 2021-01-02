package de.axxepta.converterservices.utils;

import com.jcraft.jsch.*;
import org.apache.commons.net.ftp.*;
import org.apache.commons.net.util.TrustManagerUtils;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Vector;

public class FTPUtils {

    public static final String DIR_ENTRY    = "dir ";
    public static final String FILE_ENTRY   = "file ";

    private static final int BUFFER_SIZE = 4096;

    public enum Protocol {
        FTP,
        SFTP,
        FTPS
    }

    public static String list(boolean secure, String user, String pwd, String server, int port, String path, Protocol protocol, int... timeout) throws Exception {
        StringBuilder builder = new StringBuilder();

        // ToDo: same format for sftp and ftp
        if (secure) {
            if (protocol.equals(Protocol.SFTP)) {
                String relPath = path.startsWith("/") ? path.substring(1) : path;
                JSch jsch = new JSch();
                Session session = jsch.getSession(user, server, port);
                if (timeout.length > 0) {
                    session.setTimeout(timeout[0] * 1000);
                }
                try {
                    ChannelSftp channel = getSftpChannel(session, pwd);
                    channel.connect();
                    try {
                        Vector<ChannelSftp.LsEntry> fileList = channel.ls(relPath);
                        Arrays.stream(fileList.toArray())
                                .filter(e -> ((ChannelSftp.LsEntry) e).getAttrs().isDir())
                                .forEach(e -> builder.append(DIR_ENTRY).append(((ChannelSftp.LsEntry) e).getFilename()).append("\n"));
                        Arrays.stream(fileList.toArray())
                                .filter(e -> !((ChannelSftp.LsEntry) e).getAttrs().isDir())
                                .forEach(e -> builder.append(FILE_ENTRY).append(((ChannelSftp.LsEntry) e).getFilename()).append("\n"));
                    } catch (SftpException se) {
                        channel.exit();
                        throw se;
                    }
                    channel.exit();
                } catch (JSchException | SftpException js) {
                    session.disconnect();
                    throw js;
                }
                session.disconnect();
            } else {
                FTPSClient ftpClient = new FTPSClient();
                if (timeout.length > 0) {
                    ftpClient.setConnectTimeout(timeout[0] * 1000);
                }

                try {
                    ftpClient.setTrustManager(TrustManagerUtils.getAcceptAllTrustManager());
                    ftpClient.setControlKeepAliveTimeout(300);
                    ftpClient.connect(server);
                    int reply = ftpClient.getReplyCode();
                    if(!FTPReply.isPositiveCompletion(reply)) {
                        ftpClient.disconnect();
                        throw new IOException("FTP server refused connection.");
                    }
                    if (ftpClient.login(user, pwd)) {
                        ftpClient.execPBSZ(0);
                        ftpClient.execPROT("P");
                        ftpClient.enterLocalPassiveMode();

                        FTPFile[] dirs = ftpClient.listDirectories(path);
                        FTPFile[] files = ftpClient.listFiles(path);
                        Arrays.stream(dirs).forEach(e -> builder.append(DIR_ENTRY).append(e.getName()).append("\n"));
                        Arrays.stream(files).forEach(e -> builder.append(FILE_ENTRY).append(e.getName()).append("\n"));
                        ftpClient.logout();
                    }
                } finally {
                    if(ftpClient.isConnected()) {
                        ftpClient.disconnect();
                    }
                }
            }
        } else {
            FTPClient ftpClient = new FTPClient();
            if (timeout.length > 0) {
                ftpClient.setConnectTimeout(timeout[0] * 1000);
            }
            try {
                ftpClient.enterLocalPassiveMode();
                ftpClient.connect(server, port);
                ftpClient.login(user, pwd);
                FTPFile[] dirs = ftpClient.listDirectories(path);
                FTPFile[] files = ftpClient.listFiles(path);
                Arrays.stream(dirs).forEach(e -> builder.append(DIR_ENTRY).append(e.getName()).append("\n"));
                Arrays.stream(files).forEach(e -> builder.append(FILE_ENTRY).append(e.getName()).append("\n"));
            } finally {
                ftpClient.logout();
                ftpClient.disconnect();
            }
        }
        return builder.toString();
    }

    public static String download(boolean secure, String user, String pwd, String server, int port, String path,
                                  String storePath, Protocol protocol, int... timeout)
            throws Exception
    {
        if (secure) {
            String relPath = path.startsWith("/") ? path.substring(1) : path;
            if (protocol.equals(Protocol.SFTP)) {

                JSch jsch = new JSch();
                Session session = jsch.getSession(user, server, port);
                if (timeout.length > 0) {
                    session.setTimeout(timeout[0] * 1000);
                }
                try {
                    ChannelSftp channel = getSftpChannel(session, pwd);
                    channel.connect();
                    try {
                        channel.cd(IOUtils.dirFromPath(relPath));
                        try (BufferedInputStream is = new BufferedInputStream(channel.get(IOUtils.filenameFromPath(path)))) {
                            IOUtils.copyStreamToFile(is, storePath);
                        }
                    } catch (SftpException se) {
                        channel.exit();
                        throw se;
                    }
                    channel.exit();
                } catch (JSchException | SftpException | IOException js) {
                    session.disconnect();
                    throw js;
                }
                session.disconnect();
            } else {
                FTPSClient ftpClient = new FTPSClient();
                try {
                    ftpClient.setTrustManager(TrustManagerUtils.getAcceptAllTrustManager());
                    ftpClient.setControlKeepAliveTimeout(300);
                    ftpClient.connect(server);
                    int reply = ftpClient.getReplyCode();
                    if(!FTPReply.isPositiveCompletion(reply)) {
                        ftpClient.disconnect();
                        throw new IOException("FTP server refused connection.");
                    }
                    if (ftpClient.login(user, pwd)) {
                        ftpClient.execPBSZ(0);
                        ftpClient.execPROT("P");
                        ftpClient.changeWorkingDirectory(IOUtils.dirFromPath(relPath));
                        if (ftpClient.getReplyString().contains("250")) {
                            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                            ftpClient.enterLocalPassiveMode();
                            try (OutputStream output = new FileOutputStream(storePath)) {
                                ftpClient.retrieveFile(IOUtils.filenameFromPath(path), output);
                            }
                        }
                        ftpClient.logout();
                    }
                } finally {
                    if(ftpClient.isConnected()) {
                        ftpClient.disconnect();
                    }
                }
            }
        } else {
            URL url = new URL(("ftp://") + user + ":" + pwd + "@" + server + (path.startsWith("/") ? "" : "/") + path);
            save(url, storePath);
        }
        return storePath;
    }

    private static ChannelSftp getSftpChannel(Session session, String pwd) throws JSchException {
        session.setPassword(pwd);
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect();
        return  (ChannelSftp) session.openChannel("sftp");
    }

    private static void save(URL url, String fileName) throws IOException {
        try (InputStream is = url.openStream()) {
            IOUtils.copyStreamToFile(is, fileName);
        }
    }

    private static ByteArrayOutputStream download(boolean secure, String user, String pwd, String server, int port, String path)
            throws IOException
    {
        URL url = new URL((secure ? "sftp://" : "ftp://") + user + ":" + pwd + "@" + server + path);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (InputStream is = url.openStream()) {
            int read;
            byte[] bytes = new byte[BUFFER_SIZE];
            while ((read = is.read(bytes)) != -1) {
                os.write(bytes, 0, read);
            }
        }
        return os;
    }

    public static String upload(boolean secure, String user, String pwd, String server, int port, String serverBase,
                                String localBase, String sourcePath, Protocol protocol, int... timeout)
            throws IOException, JSchException, SftpException
    {
        String relPath = IOUtils.pathCombine(serverBase, IOUtils.relativePath(sourcePath, localBase));
        String relativePath = relPath.replaceAll("\\\\", "/");
        File file = new File(sourcePath);
        if (secure) {
            relPath = IOUtils.dirFromPath(relPath).replaceAll("\\\\", "/");
            String path = relPath.startsWith("/") ? relPath.substring(1) : relPath;
            if (protocol.equals(Protocol.SFTP)) {
                JSch jsch = new JSch();
                Session session = jsch.getSession(user, server, port);
                if (timeout.length > 0) {
                    session.setTimeout(timeout[0] * 1000);
                }
                try {
                    ChannelSftp channel = getSftpChannel(session, pwd);
                    channel.connect();
                    try {
                        channel.cd(path);
                        // ToDo: check whether non-existent directories will be created
                        channel.put(new FileInputStream(file), IOUtils.filenameFromPath(sourcePath), ChannelSftp.OVERWRITE);
                    } catch (SftpException se) {
                        channel.exit();
                        throw se;
                    }
                    channel.exit();
                } catch (JSchException | SftpException js) {
                    session.disconnect();
                    throw js;
                }
                session.disconnect();
            } else {

                FTPSClient ftpClient = new FTPSClient();
                try {
                    ftpClient.setTrustManager(TrustManagerUtils.getAcceptAllTrustManager());
                    ftpClient.setControlKeepAliveTimeout(300);
                    ftpClient.connect(server);
                    int reply = ftpClient.getReplyCode();
                    if(!FTPReply.isPositiveCompletion(reply)) {
                        ftpClient.disconnect();
                        throw new IOException("FTP server refused connection.");
                    }
                    if (ftpClient.login(user, pwd)) {
                        ftpClient.execPBSZ(0);
                        ftpClient.execPROT("P");
                        ftpClient.changeWorkingDirectory(path);
                        if (ftpClient.getReplyString().contains("250")) {
                            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                            ftpClient.enterLocalPassiveMode();
                            try (FileInputStream is = new FileInputStream(sourcePath)) {
                                ftpClient.storeFile(IOUtils.filenameFromPath(sourcePath), is);
                            }
                        }
                        ftpClient.logout();
                    }
                } finally {
                    if(ftpClient.isConnected()) {
                        ftpClient.disconnect();
                    }
                }
            }
        } else {
            relPath = relPath.replaceAll("\\\\", "/");
            String path = (relPath.startsWith("/") ? "" : "/") + relPath;
            URL url = new URL("ftp://" + user + ":" + pwd + "@" + server + path);
            URLConnection conn = url.openConnection();
            if (timeout.length > 0) {
                conn.setConnectTimeout(timeout[0] * 1000);
            }
            transmit(conn, sourcePath);
        }
        return "<success>Uploaded " + sourcePath + " to " + (secure ? "sftp://" : "ftp://") + server +
                (relativePath.startsWith("/") ? "" : "/") + relativePath + "</success>";
    }

    private static void transmit(URLConnection connection, String file) throws IOException {
        try (OutputStream os = connection.getOutputStream()) {
            try (FileInputStream is = new FileInputStream(file)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
        }
    }

}
