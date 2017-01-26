package de.axxepta.sparkservices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.SimpleLogger;
import spark.Request;
import spark.Response;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static spark.Spark.*;

public class App {

    private static Logger logger;

    private static String TEMP_FILE_PATH = System.getProperty("user.home") + "/.sparkservices";

    private static final String THUMB_UPLOAD_FORM       = "static/form_thumb.html";
    private static final String THUMBS_UPLOAD_FORM      = "static/form_thumbs.html";
    private static final String META_UPLOAD_FORM        = "static/form_meta.html";

    private static final String TYPE_JPEG               = "image/jpeg";

    private static final String FILE_PART               = "FILE";
    private static final String MULTI_PART_BOUNDARY     = "MULTI_PART_BOUNDARY";
    private static final String HTML_OPEN               = "<html><body><h1>";
    private static final String HTML_CLOSE              = "</h1></body></html>";
    private static final String NO_SUCH_FILE            = "<html><body><h1>File does not exist.</h1></body></html>";
    private static final String NO_FILES_JSON           = "{\"ERROR\": \"No temporary file found.\"}";
    private static final String EXCEPTION_OPEN_JSON     = "{\"EXCEPTION\": \"";
    private static final String EXCEPTION_CLOSE_JSON    = "\"}";


    public static void main(String[] args) {

        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "ERROR");
        logger = LoggerFactory.getLogger(App.class);
        try {
            safeCreateDirectory(TEMP_FILE_PATH);
        } catch (IOException ie) {
            logger.error("Couldn't create directory for temporary files!");
        }

        get("/hello", (request, response) -> "hello");

        post("/file/thumb", "multipart/form-data", (request, response) -> {
            List<String> files;
            try {
                files = thumbifyFiles(request);
            } catch (IOException | InterruptedException ex) {
                return HTML_OPEN + ex.getMessage() + HTML_CLOSE;
            }

            if (files.size() > 0) {
                HttpServletResponse raw = singleFileResponse(response, request.params("thumb_" + jpgFilename(files.get(0))));
                File file = new File(TEMP_FILE_PATH + "/" + jpgFilename(files.get(0)));
                if (file.exists()) {
                    files.add(jpgFilename(files.get(0)));
                    try (InputStream is = new FileInputStream(file)) {
                        copyStreams(is, raw.getOutputStream());
                        raw.getOutputStream().close();
                        return raw;
                    } catch (Exception e) {
                        return HTML_OPEN + e.getMessage() + HTML_CLOSE;
                    } finally {
                        cleanTemp(files);
                    }
                } else {
                    cleanTemp(files);
                    return NO_SUCH_FILE;
                }
            } else {
                return NO_SUCH_FILE;
            }
        });

        post("/file/thumbs", "multipart/form-data", (request, response) -> {
            List<String> files;
            try {
                files = thumbifyFiles(request);
            } catch (IOException | InterruptedException ex) {
                return HTML_OPEN + ex.getMessage() + HTML_CLOSE;
            }

            if (files.size() > 0) {
                try {
                    List<String> transformedFiles = new ArrayList<>();
                    HttpServletResponse raw = multiPartResponse(response);
                    for (String fileName : files) {
                        File file = new File(TEMP_FILE_PATH + "/" + jpgFilename(fileName));
                        if (file.exists()) {
                            transformedFiles.add(jpgFilename(fileName));
                            try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
                                addMultiPartFile(raw.getOutputStream(), TYPE_JPEG, is, "thumb_" + jpgFilename(fileName));
                            }
                        }
                    }
                    files.addAll(transformedFiles);
                    multiPartClose(raw.getOutputStream());
                    return raw;
                } catch (IOException ex) {
                    return HTML_OPEN + ex.getMessage() + HTML_CLOSE;
                } finally {
                    cleanTemp(files);
                }
            } else {
                return NO_SUCH_FILE;
            }
        });

        get("/static/:name", (request, response) -> {
            HttpServletResponse raw = singleFileResponse(response, request.params(":name"));
            File file = new File(TEMP_FILE_PATH + "/" + request.params(":name"));
            if (file.exists()) {
                try (InputStream is = new FileInputStream(file)) {
                    copyStreams(is, raw.getOutputStream());
                    raw.getOutputStream().close();
                } catch (Exception e) {
                    return HTML_OPEN + e.getMessage() + HTML_CLOSE;
                }
            } else {
                return NO_SUCH_FILE;
            }
            return raw;
        });

        get("/file/thumbupload", (request, response) ->
                String.format(getResource(THUMB_UPLOAD_FORM), FILE_PART)
        );

        get("/files/thumbupload", (request, response) ->
                String.format(getResource(THUMBS_UPLOAD_FORM), FILE_PART)
        );

        get("/file/metaupload", (request, response) ->
                String.format(getResource(META_UPLOAD_FORM), FILE_PART)
        );

        post("/files/upload", "multipart/form-data", (request, response) -> {
            MultipartConfigElement multipartConfigElement = new MultipartConfigElement(TEMP_FILE_PATH);
            request.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);

            Collection<Part> parts = null;
            try {
                parts = request.raw().getParts();
            } catch (IOException | ServletException ex) {
                ex.printStackTrace();
            }
            StringBuilder builder = new StringBuilder("<html><body>");
            if (parts != null) {
                for (Part part : parts) {
                    builder.append("<p>Name: ").append(part.getName()).append("<br>");
                    builder.append("Size: ").append(part.getSize()).append("<br>");
                    builder.append("Filename: ").append(part.getSubmittedFileName()).append("<br></p>");
                    part.write(part.getSubmittedFileName());
                }
            }
            return builder.append("</body></html>").toString();
        });

        post("/file/meta", (request, response) -> {
            List<String > files = storeFilesTemporary(request);
            try {
                if (files.size() > 0) {
                    ByteArrayOutputStream out =
                            runExternal("exiftool", "-e", "-json", TEMP_FILE_PATH + "/" + files.get(0));
                    cleanTemp(files);
                    return new String(out.toByteArray(), "UTF-8");
                } else return NO_FILES_JSON;
            } catch (Exception e) {
                return EXCEPTION_OPEN_JSON + e.getMessage() + EXCEPTION_CLOSE_JSON;
            }
        });

        get("/stop", (request, response) -> {
            stop();
            return "Services stopped.";
        });

    }


    private static List<String> thumbifyFiles(Request request) throws IOException, InterruptedException {
        List<String> files = storeFilesTemporary(request);
        try {
            for (String file : files) {
                runExternal("mogrify", "-flatten", "-strip", "-format", "jpg", "-quality",
                        "75", "-thumbnail", "300x300^", "-gravity", "center", "-extent", "300x300",  TEMP_FILE_PATH + "/" + file);
            }
        } catch (IOException | InterruptedException ex) {
            cleanTemp(files);
            throw ex;
        }
        return files;
    }

    private static HttpServletResponse singleFileResponse(Response response, String fileName) {
        HttpServletResponse raw = response.raw();
        response.header("Content-Disposition", "attachment; filename=" + fileName);
        response.type("application/force-download");
        return raw;
    }

    private static HttpServletResponse multiPartResponse(Response response) throws IOException {
        HttpServletResponse raw = response.raw();
        response.type("multipart/x-mixed-replace;boundary=" + MULTI_PART_BOUNDARY);
        ServletOutputStream os = raw.getOutputStream();
        os.println();
        os.println("--" + MULTI_PART_BOUNDARY);
        return raw;
    }

    private static void addMultiPartFile(ServletOutputStream os, String contentType, InputStream is, String name) throws IOException {
        os.println("--" + MULTI_PART_BOUNDARY);
        os.println("Content-Disposition: attachment; filename=\"" + name + "\"");
        os.println("Content-Type: " + contentType);
        os.println();
        copyStreams(is, os);
        os.println("--" + MULTI_PART_BOUNDARY);     // ?
        os.flush();
    }

    private static void multiPartClose(ServletOutputStream os) throws IOException {
        os.println();
        os.flush();
        os.println("--" + MULTI_PART_BOUNDARY + "--");
        os.close();
    }

    private static String jpgFilename(String name) {
        int pos = name.lastIndexOf(".");
        return name.substring(0, (pos == -1) ? name.length() : pos) + ".jpg";
    }

    private static void copyStreams(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer, 0, buffer.length)) > -1) {
            os.write(buffer, 0, length);
            os.flush();
        }
    }

    private static List<String> storeFilesTemporary(Request request) {
        MultipartConfigElement multipartConfigElement = new MultipartConfigElement(TEMP_FILE_PATH);
        request.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);
        Collection<Part> parts;
        List<String> files = new ArrayList<>();
        try {
            parts = request.raw().getParts();
            if (parts != null) {
                for (Part part : parts) {
                    if (part.getName().equals(FILE_PART)) {
                        part.write(part.getSubmittedFileName());
                        files.add(part.getSubmittedFileName());
                    }
                }
            }
        } catch (IOException | ServletException ex) {
            if (logger != null) logger.error(ex.getMessage());
        }
        return files;
    }

    private static void cleanTemp(List<String> files) {
        try {
            for (String file : files) {
                Files.delete(Paths.get(TEMP_FILE_PATH + "/" + file));
            }
        } catch (IOException ex) {
            if (logger != null) logger.error(ex.getMessage());
        }
    }

    private static ByteArrayOutputStream runExternal(String... command) throws IOException, InterruptedException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[1024];
            Process p = Runtime.getRuntime().exec(command);
            try (InputStream is = p.getInputStream()) {
                int n;
                while ((n = is.read(buffer)) > -1) {
                    stream.write(buffer, 0, n);
                }
            }
            String line;
            try (BufferedReader bre = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
                while ((line = bre.readLine()) != null) {
                    if (logger != null) logger.error(line);
                }
            }
            int code = p.waitFor();
            if (logger != null) logger.debug("External program returned with code " + code);
        } catch (IOException | InterruptedException err) {
            if (logger != null) logger.error(err.getMessage());
            throw err;
        }
        return stream;
    }

    private static void safeCreateDirectory(String path) throws IOException {
        if (!Files.exists(Paths.get(path)))
            Files.createDirectory(Paths.get(path));
    }

    private static String getResource(String name) throws IOException {
        final String resource = "/" + name;
        final InputStream is = App.class.getResourceAsStream(resource);
        if(is == null) throw new IOException("Resource not found: " + resource);
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

}
