package de.axxepta.converterservices;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

class IOUtils {

    private IOUtils() {}


    static void copyStreams(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer, 0, buffer.length)) > -1) {
            os.write(buffer, 0, length);
            os.flush();
        }
    }

    static String getResource(String name) throws IOException {
        final String resource = "/" + name;
        final InputStream is = IOUtils.class.getResourceAsStream(resource);
        if(is == null) throw new IOException("Resource not found: " + resource);
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    static void safeCreateDirectory(String path) throws IOException {
        if (!Files.exists(Paths.get(path)))
            Files.createDirectory(Paths.get(path));
    }

    static String dirFromPath(String path) {
        int sepPos = path.lastIndexOf("/");
        return path.substring(0, Math.max(0, sepPos));
    }
}
