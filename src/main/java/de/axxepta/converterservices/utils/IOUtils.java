package de.axxepta.converterservices.utils;

import de.axxepta.converterservices.App;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class IOUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(IOUtils.class);

    private static String hostName = null;

    public static boolean pathExists(String path) {
        File f = new File(path);
        return f.exists();
    }

    public static boolean isFile(String path) {
        File f = new File(path);
        return (f.exists() && f.isFile());
    }

    public static boolean isDirectory(String path) {
        File f = new File(path);
        return (f.exists() && f.isDirectory());
    }

    public static void copyStreams(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[8192];
        int length;
        while ((length = is.read(buffer, 0, buffer.length)) > -1) {
            os.write(buffer, 0, length);
        }
    }

    public static void byteArrayToFile(byte[] ba, String destination) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(destination)) {
            fos.write(ba);
        }
    }

    public static void byteArrayOutputStreamToFile(ByteArrayOutputStream os, String destination) throws IOException {
        try(OutputStream outputStream = new FileOutputStream(destination)) {
            os.writeTo(outputStream);
        }
    }

    public static void copyStreamToFile(InputStream is, String destination) throws IOException {
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(destination))) {
            copyStreams(is, os);
        }
    }

    public static void saveStringToFile(String line, String fileName, String... characterSet) throws IOException {
        String charset = characterSet.length > 0 ? characterSet[0] : "UTF-8";
        try (PrintWriter writer = new PrintWriter(fileName, charset)) {
            writer.print(line);
        }
    }

    public static void saveStringArrayToFile(List<String> lines, String fileName, boolean removeEmptyLines,
                                             String... characterSet) throws IOException {
        String charset = characterSet.length > 0 ? characterSet[0] : "UTF-8";
        try (PrintWriter writer = new PrintWriter(fileName, charset)) {
            int i = 1;
            for (String line : lines) {
                if (!(removeEmptyLines && StringUtils.isNoStringOrEmpty(line))) {
                    if (i < lines.size()) {
                        writer.println(line);
                    } else {
                        writer.print(line);
                    }
                }
                i++;
            }
        }
    }

    public static byte[] loadByteArrayFromFile(String fileName) throws IOException {
        Path fileLocation = Paths.get(fileName);
        return Files.readAllBytes(fileLocation);
    }

    public static String loadStringFromFile(String fileName, String... charset) throws IOException {
        String encoding = charset.length > 0 ? charset[0] : "UTF-8";
        return new String(Files.readAllBytes(Paths.get(fileName)), encoding);
    }

    public static String getResourceAsString(String name) throws IOException {
        return getResourceAsString(name, IOUtils.class);
    }

    public static String getResourceAsString(String name, Class referenceClass) throws IOException {
        final String resource = "/" + name;
        final InputStream is = referenceClass.getResourceAsStream(resource);
        if(is == null) throw new IOException("Resource not found: " + resource);
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    /**
     * Copies resources from a folder in a jar file to a sub-directory on disk with the same name.
     * If the target path does not exist it will be created.
     * @param path Target base path
     * @param resourcePath JAR/target folder name of the resources
     * @param referenceClass Class in the jar file containing the resources
     * @throws IOException Creation of directories or copying can throw an exception.
     */
    public static void copyResources(String path, String resourcePath, Class referenceClass) throws IOException {
        IOUtils.safeCreateDirectory(path);
        IOUtils.safeCreateDirectory(IOUtils.pathCombine(path, resourcePath));
        File jarFile = new File(referenceClass.getProtectionDomain().getCodeSource().getLocation().getPath());
        JarFile jar = new JarFile(jarFile);
        final Enumeration<JarEntry> entries = jar.entries();
        while(entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.startsWith(resourcePath)) {
                if (entry.isDirectory()) {
                    IOUtils.safeCreateDirectory(pathCombine(path, name));
                } else {
                    copyResource(name, path, referenceClass);
                }
            }
        }
        jar.close();
    }

    private static void copyResource(final String fileName, final String target, Class referenceClass) throws IOException {
        String path = target + IOUtils.dirFromPath(fileName);
        if (!Files.exists(Paths.get(path)))
            new File(path).mkdirs();
        InputStream is = referenceClass.getResourceAsStream("/" + fileName);
        if(is == null) throw new IOException("Resource not found: " + fileName);
        Files.copy(is,
                Paths.get(target + (target.endsWith("/") || target.endsWith("\\") ? "" : "/") + fileName),
                REPLACE_EXISTING );
        is.close();
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

    public static boolean renameFile(String oldName, String newName) throws IOException {
        File fileOld = new File(oldName);
        File fileNew = new File(newName);
        if (fileNew.exists())
            throw new java.io.IOException(String.format("File %s already exists", newName));
        return fileOld.renameTo(fileNew);
    }

    public static String relativePath(File file, String basePath) throws IOException {
        return relativePath(file.getCanonicalFile(), basePath);
    }

    public static String relativePath(String file, String basePath) {
        String[] fileParts = file.split("/|\\\\");
        String[] baseParts = basePath.split("/|\\\\");
        if (isWin() ? fileParts[0].toLowerCase().equals(baseParts[0].toLowerCase()) : fileParts[0].equals(baseParts[0])) {
            int same = 0;
            while (fileParts.length > same && baseParts.length > same &&
                    (isWin() ? fileParts[same].toLowerCase().equals(baseParts[same].toLowerCase()) :
                            fileParts[same].equals(baseParts[same]) )
                    ) {
                same++;
            }
            List<String> builder = new ArrayList<>();
            for (int i = same; i < baseParts.length; i++) {
                builder.add("..");
            }
            builder.addAll(Arrays.asList(Arrays.copyOfRange(fileParts, same, fileParts.length)));
/*            for (int i = same; i < fileParts.length; i++) {
                builder.add(fileParts[i]);
            }*/
            return String.join("/", builder);
        } else {
            throw new IllegalArgumentException("Given path is not in base path.");
        }
    }

    public static String dirFromPath(String path) {
        int sepPos = Math.max(path.lastIndexOf("/"), path.lastIndexOf("\\"));
        return path.substring(0, Math.max(0, sepPos));
    }

    public static String filenameFromPath(String path) {
        String[] parts = path.split("/|\\\\");
        return parts[parts.length - 1];
    }

    public static String strippedFilename(String path) {
        String fileName = filenameFromPath(path);
        int p = fileName.lastIndexOf(".");
        return p == -1 ? fileName : fileName.substring(0, p);
    }

    public static String getFileExtension(String path) {
        int sepPos = path.lastIndexOf(".");
        if (sepPos == -1) {
            return "";
        } else {
            return path.substring(sepPos + 1);
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

    public static String contentTypeByFileName(String fileName) {
        switch (IOUtils.getFileExtension(fileName).toLowerCase()) {
            case "jpg": return App.TYPE_JPEG;
            case "png": return App.TYPE_PNG;
            case "pdf": return App.TYPE_PDF;
            case "xlsx": return App.TYPE_XLSX;
            case "csv": return App.TYPE_CSV;
            case "xml": return App.TYPE_XML;
            default: return App.TYPE_OCTET;
        }
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

    public static String jarPath() {
        String path;
        try {
            path = new File(IOUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
        } catch (URISyntaxException ue) {
            path = IOUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        }
        return dirFromPath(path);
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
     * @param logFunction optional logging function/lambda (String) -> void
     * @return All absolute file names including all files in sub-directories
     * @throws IOException if a provided input element corresponds not to an existing directory or file
     */
    @SafeVarargs
    public static List<String> collectFiles(List<String> input, Consumer<String>... logFunction) throws IOException {
        List<String> output = new ArrayList<>();
        for (String inFile : input) {
            if (IOUtils.pathExists(inFile)) {
                if (IOUtils.isDirectory(inFile)) {
                    addSubDirFiles(output, inFile);
                } else {
                    output.add(inFile);
                }
            } else {
                if (logFunction.length > 0) {
                    logFunction[0].accept(String.format("File or directory %s not found.", inFile));
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
