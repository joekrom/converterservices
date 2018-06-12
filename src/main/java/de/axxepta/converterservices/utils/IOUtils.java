package de.axxepta.converterservices.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class IOUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(IOUtils.class);

    private static String hostName = null;

    // enable use of static functions by BaseX XQuery
    public IOUtils() {}

    public static boolean fileExists(String path) {
        File f = new File(path);
        return f.exists();
    }

    public static boolean isDirectory(String path) {
        File f = new File(path);
        return (f.exists() && f.isDirectory());
    }

    public static void copyStreams(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer, 0, buffer.length)) > -1) {
            os.write(buffer, 0, length);
            os.flush();
        }
    }

    public static void ByteArrayOutputStreamToFile(ByteArrayOutputStream os, String destination) throws IOException {
        try(OutputStream outputStream = new FileOutputStream(destination)) {
            os.writeTo(outputStream);
        }
    }

    public static void copyStreamToFile(InputStream is, String destination) throws IOException {
        try (OutputStream os = new FileOutputStream(destination)) {
            copyStreams(is, os);
        }
    }

    public static String getResource(String name) throws IOException {
        final String resource = "/" + name;
        final InputStream is = IOUtils.class.getResourceAsStream(resource);
        if(is == null) throw new IOException("Resource not found: " + resource);
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static void safeCreateDirectory(String path) throws IOException {
        if (!Files.exists(Paths.get(path)))
            Files.createDirectory(Paths.get(path));
    }

    public static void safeDeleteFile(String path) {
        Path filePath = Paths.get(path);
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException io) {
            LOGGER.warn("File could not be deleted: ", io);
        }
    }

    public static String dirFromPath(String path) {
        int sepPos = path.lastIndexOf(File.separator);
        return path.substring(0, Math.max(0, sepPos));
    }

    public static String filenameFromPath(String path) {
        String[] components = path.split("/|\\\\");
        return components[components.length - 1];
    }

    public static String readTextFile(String fileName) throws FileNotFoundException, IOException {
        try(BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            return sb.toString();
        }
    }

    public static boolean isXLSX(String fileName) {
        return fileName.toLowerCase().endsWith(".xlsx");
    }

    public static boolean isCSV(String fileName) {
        return fileName.toLowerCase().endsWith(".csv");
    }

    public static boolean isXML(String fileName) {
        return fileName.toLowerCase().endsWith(".xml");
    }

    public static String pathCombine(String stComp, String ndComp) {
        String[] startDirs = stComp.split("\\\\|/");
        String[] endDirs = ndComp.split("\\\\|/");
        int up = 0;
        while ((up < endDirs.length) && endDirs[up].equals("..")) {
            up++;
        }
        if (up > startDirs.length)
            throw new IllegalStateException("Paths cannot be combined to valid path");
        List<String> compList = Arrays.asList(startDirs).subList(0, startDirs.length - 1 - up);
        compList.addAll(Arrays.asList(endDirs).subList(up, endDirs.length - 1));
        return String.join(File.separator, compList);
    }

    public static boolean isWin() {
        return System.getProperty("os.name").startsWith("Windows");
    }

    public static String getHostName() {
        if (hostName != null) {
            return hostName;
        } else {
            hostName = hostName();
            return hostName;
        }
    }

    private static String hostName() {
        Map<String, String> env = System.getenv();
        if (env.containsKey("COMPUTERNAME")) {
            return env.get("COMPUTERNAME");
        } else if (env.containsKey("HOSTNAME")) {
            return env.get("HOSTNAME");
        } else {
            try {
                InetAddress address;
                address = InetAddress.getLocalHost();
                return address.getHostName();
            } catch (UnknownHostException ex) {
                return "Hostname Unknown";
            }
        }
    }

}
