package de.axxepta.converterservices.utils;

import com.jcraft.jsch.*;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Vector;

public class FTPUtils {

    public static final String DIR_ENTRY    = "dir ";
    public static final String FILE_ENTRY   = "file ";

    private static final int BUFFER_SIZE = 4096;

    public static String list(boolean secure, String user, String pwd, String server, int port, String path, int... timeout) throws Exception {
        StringBuilder builder = new StringBuilder();

        // ToDo: same format for sftp and ftp
        if (secure) {
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
            FTPClient ftpClient = new FTPClient();
            if (timeout.length > 0) {
                ftpClient.setConnectTimeout(timeout[0] * 1000);
            }
            IOException ex = null;
            try {
                ftpClient.enterLocalPassiveMode();
                ftpClient.connect(server, port);
                ftpClient.login(user, pwd);
                FTPFile[] dirs = ftpClient.listDirectories(path);
                FTPFile[] files = ftpClient.listFiles(path);
                Arrays.stream(dirs).forEach(e -> builder.append(DIR_ENTRY).append(e.getName()).append("\n"));
                Arrays.stream(files).forEach(e -> builder.append(FILE_ENTRY).append(e.getName()).append("\n"));
            } catch (IOException e) {
                ex = e;
            } finally {
                ftpClient.logout();
                ftpClient.disconnect();
            }
            if (ex != null)
                throw ex;
        }
        return builder.toString();
    }

    public static String download(boolean secure, String user, String pwd, String server, int port, String path,
                                  String storePath, int... timeout)
            throws Exception
    {
        if (secure) {
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
                                String localBase, String sourcePath, int... timeout)
            throws IOException, JSchException, SftpException
    {
        String relPath = IOUtils.pathCombine(serverBase, IOUtils.relativePath(sourcePath, localBase));
        String relativePath = relPath.replaceAll("\\\\", "/");
        File file = new File(sourcePath);
        if (secure) {
            relPath = IOUtils.dirFromPath(relPath).replaceAll("\\\\", "/");
            String path = relPath.startsWith("/") ? relPath.substring(1) : relPath;
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
