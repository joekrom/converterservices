package de.axxepta.converterservices.utils;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;

public class FTPUtils {

    private static final int BUFFER_SIZE = 4096;

    public static String list(String user, String pwd, String server, int port, String path) throws IOException {
        FTPClient ftpClient = new FTPClient();
        StringBuilder builder = new StringBuilder();
        IOException ex = null;
        try {
            ftpClient.enterLocalPassiveMode();
            ftpClient.connect(server, port);
            ftpClient.login(user, pwd);
            FTPFile[] dirs  = ftpClient.listDirectories(path);
            FTPFile[] files = ftpClient.listFiles(path);
            Arrays.stream(dirs).forEach(e->builder.append("dir ").append(e.getName()).append("\n"));
            Arrays.stream(files).forEach(e->builder.append("file ").append(e.getName()).append("\n"));
        } catch (IOException e) {
            ex = e;
        } finally {
            ftpClient.logout();
            ftpClient.disconnect();
        }
        if (ex != null)
            throw ex;
        return builder.toString();
    }

    public static String download(boolean secure, String user, String pwd, String server, String path, String storePath)
            throws IOException
    {
        System.out.println((secure ? "sftp://" : "ftp://") + user + ":" + pwd + "@" + server + path);
        URL url = new URL((secure ? "sftp://" : "ftp://") + user + ":" + pwd + "@" + server + path);
        save(url, storePath);
        return storePath;
    }

    private static void save(URL url, String fileName) throws IOException {
        try (InputStream is = url.openStream()) {
            try (OutputStream os = new FileOutputStream(new File(fileName)) ) {
                int read;
                byte[] bytes = new byte[BUFFER_SIZE];
                while ((read = is.read(bytes)) != -1) {
                    os.write(bytes, 0, read);
                }
            }
        }
    }

    private static ByteArrayOutputStream download(boolean secure, String user, String pwd, String server, String path)
            throws IOException
    {
        URL url = new URL((secure ? "ftps://" : "ftp://") + user + ":" + pwd + "@" + server + path);
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


    public static String upload(boolean secure, String user, String pwd, String server, String path, String sourcePath)
            throws IOException
    {

        System.out.println((secure ? "sftp://" : "ftp://") + user + ":" + pwd + "@" + server + path);
        URL url = new URL((secure ? "sftp://" : "ftp://") + user + ":" + pwd + "@" + server + path);
        URLConnection conn = url.openConnection();
        transmit(conn, sourcePath);
        return "<success>Uploaded " + sourcePath + " to " + "ftp://" + server + path + "</success>";
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
