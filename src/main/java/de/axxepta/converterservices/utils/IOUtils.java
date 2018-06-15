package de.axxepta.converterservices.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class IOUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(IOUtils.class);

    private static String hostName = null;

    public static boolean pathExists(String path) {
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

    public static void saveStringToFile(String line, String fileName) throws IOException {
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write(line);
        }
    }

    public static void saveStringArrayToFile(List<String> lines, String fileName) throws IOException {
        try (FileWriter writer = new FileWriter(fileName)) {
            for (String line : lines) {
                writer.write(line);
            }
        }
    }

    public static String loadStringFromFile(String fileName) throws IOException {
        return new String(Files.readAllBytes(Paths.get(fileName)), StandardCharsets.UTF_8);
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

    public static String relativePath(File file, String basePath) throws IOException {
        return relativePath(file.getCanonicalFile(), basePath);
    }

    public static String relativePath(String file, String basePath) {
        String[] fileParts = file.split("/|\\\\");
        String[] baseParts = basePath.split("/|\\\\");
        if (fileParts[0].equals(baseParts[0])) {
            int same = 0;
            while (fileParts.length > same && baseParts.length > same && fileParts[same].equals(baseParts[same])) {
                same++;
            }
            List<String> builder = new ArrayList<>();
            for (int i = same; i < baseParts.length; i++) {
                builder.add("..");
            }
            for (int i = same; i < fileParts.length; i++) {
                builder.add(fileParts[i]);
            }
            return String.join("/", builder);
        } else {
            throw new IllegalArgumentException("Given path is not in base path.");
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

    public static String getFileExtension(String path) {
        int sepPos = path.lastIndexOf(".");
        if (sepPos == -1) {
            return "";
        } else {
            String[] parts = path.split("\\.");
            return parts[parts.length - 1];
        }
    }

    public static String readTextFile(String fileName) throws IOException {
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
        List<String> compList = new ArrayList<>(Arrays.asList(startDirs).subList(0, startDirs.length - up));
        List<String> remain = Arrays.asList(endDirs).subList(up, endDirs.length);
        compList.addAll(remain);
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

    private static byte[] createChecksum(String filename) throws Exception {
        MessageDigest complete = MessageDigest.getInstance("MD5");
        try (InputStream is =  new FileInputStream(filename)) {
            byte[] buffer = new byte[1024];
            int length;
            do {
                length = is.read(buffer);
                if (length > 0) {
                    complete.update(buffer, 0, length);
                }
            } while (length != -1);
        }
        return complete.digest();
    }

    public static String getMD5Checksum(String filename) throws Exception {
        byte[] checksum = createChecksum(filename);
        StringBuilder result = new StringBuilder();
        for (byte b : checksum) {
            result.append(Integer.toString( ( b & 0xff ) + 0x100, 16).substring(1));
        }
        return result.toString();
    }

    /**
     * Recursively collect all files in a list of files and paths with sub-directories with absolute paths
     * @param input List of file or directory names
     * @return All absolute file names including all files in sub-directories
     * @throws IOException
     */
    public static List<String> collectFiles(List<String> input) throws IOException {
        List<String> output = new ArrayList<>();
        for (String inFile : input) {
            if (IOUtils.pathExists(inFile)) {
                if (IOUtils.isDirectory(inFile)) {
                    addSubDirFiles(output, inFile);
                } else {
                    output.add(inFile);
                }
            }
        }
        return output;
    }

    private static void addSubDirFiles(List<String> output, String dir)
            throws IOException
    {
        File directory = new File(dir);
        File[] filesList = directory.listFiles();
        for (File file : filesList) {
            if (file.isFile()) {
                output.add(file.getCanonicalPath());
            } else {
                addSubDirFiles(output, file.getCanonicalPath());
            }
        }
    }

}
