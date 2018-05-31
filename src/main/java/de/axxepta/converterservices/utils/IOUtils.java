package de.axxepta.converterservices.utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class IOUtils {

    private IOUtils() {}


    public static void copyStreams(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer, 0, buffer.length)) > -1) {
            os.write(buffer, 0, length);
            os.flush();
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
        } catch (IOException io) {}
    }

    public static String dirFromPath(String path) {
        int sepPos = path.lastIndexOf(File.separator);
        return path.substring(0, Math.max(0, sepPos));
    }

    public static String filenameFromPath(String path) {
        int sepPos = path.lastIndexOf(File.separator);
        return path.substring(Math.max(-1, sepPos) + 1);
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
}
