package de.axxepta.converterservices;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemHeaders;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
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
import java.util.*;

import static spark.Spark.*;

public class App {

    private static Logger logger;

    private static final String STATIC_FILE_PATH = System.getProperty("user.home") + "/.converterservices";
    private static final String TEMP_FILE_PATH = STATIC_FILE_PATH + "/temp";

    private static final String PATH_HELLO              = "/hello";
    private static final String PATH_STOP               = "/stop";
    private static final String PATH_THUMB              = "/file/thumb";
    private static final String PATH_THUMBS             = "/file/thumbs";
    private static final String PATH_META               = "/file/meta";
    private static final String PATH_SPLIT              = "/pdfsplit";
    private static final String PATH_DOWNLOAD           = "/static";
    private static final String PATH_UPLOAD             = "/files/upload";
    private static final String PATH_UPLOAD_THUMB       = "/file/thumbupload";
    private static final String PATH_UPLOAD_THUMBS      = "/files/thumbupload";
    private static final String PATH_UPLOAD_META        = "/file/metaupload";
    private static final String PATH_UPLOAD_SPLIT       = "/file/pdfsplitupload";

    private static final String PARAM_NAME              = ":name";

    private static final String HELLO_PAGE              = "static/hello.html";
    private static final String THUMB_UPLOAD_FORM       = "static/form_thumb.html";
    private static final String THUMBS_UPLOAD_FORM      = "static/form_thumbs.html";
    private static final String META_UPLOAD_FORM        = "static/form_meta.html";
    private static final String PDFSPLIT_UPLOAD_FORM    = "static/form_pdfsplit.html";

    private static final String TYPE_JPEG               = "image/jpeg";
    private static final String TYPE_PNG                = "image/png";
    private static final String TYPE_PDF                = "application/pdf";

    private static final String FILE_PART               = "FILE";
    private static final String TO_IMAGE                = "IS_IMAGE";
    private static final String MULTIPART_FORM_DATA     = "multipart/form-data";
    private static final String MULTI_PART_BOUNDARY     = "MULTI_PART_BOUNDARY";
    private static final String CONTENT_TRANSFER_ENCODING     = "Content-Transfer-Encoding";
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
            de.axxepta.converterservices.IOUtils.safeCreateDirectory(TEMP_FILE_PATH);
        } catch (IOException ie) {
            logger.error("Couldn't create directory for temporary files!");
        }

        get(PATH_HELLO, (request, response) ->
                String.format(de.axxepta.converterservices.IOUtils.getResource(HELLO_PAGE), PATH_THUMB, PATH_THUMBS,
                        PATH_META, PATH_SPLIT, PATH_UPLOAD_THUMB, PATH_UPLOAD_THUMBS, PATH_UPLOAD_META, PATH_UPLOAD_SPLIT, PATH_STOP)
        );

        post(PATH_THUMB, MULTIPART_FORM_DATA, (request, response) -> {
            List<String> files;
            try {
                files = thumbifyFiles(request);
            } catch (IOException | InterruptedException ex) {
                return HTML_OPEN + ex.getMessage() + HTML_CLOSE;
            }

            if (files.size() > 0) {
                HttpServletResponse raw = singleFileResponse(response, "thumb_" + jpgFilename(files.get(0)));
                File file = new File(TEMP_FILE_PATH + "/" + jpgFilename(files.get(0)));
                if (file.exists()) {
                    files.add(jpgFilename(files.get(0)));
                    try (InputStream is = new FileInputStream(file)) {
                        de.axxepta.converterservices.IOUtils.copyStreams(is, raw.getOutputStream());
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

        post(PATH_THUMBS, MULTIPART_FORM_DATA, (request, response) -> {
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

        get(PATH_DOWNLOAD + "/" + PARAM_NAME, (request, response) -> {
            HttpServletResponse raw = singleFileResponse(response, request.params(PARAM_NAME));
            File file = new File(STATIC_FILE_PATH + "/" + request.params(PARAM_NAME));
            if (file.exists()) {
                try (InputStream is = new FileInputStream(file)) {
                    de.axxepta.converterservices.IOUtils.copyStreams(is, raw.getOutputStream());
                    raw.getOutputStream().close();
                } catch (Exception e) {
                    return HTML_OPEN + e.getMessage() + HTML_CLOSE;
                }
            } else {
                return NO_SUCH_FILE;
            }
            return raw;
        });

        get(PATH_UPLOAD_THUMB, (request, response) ->
                String.format(de.axxepta.converterservices.IOUtils.getResource(THUMB_UPLOAD_FORM), PATH_THUMB, FILE_PART)
        );

        get(PATH_UPLOAD_THUMBS, (request, response) ->
                String.format(de.axxepta.converterservices.IOUtils.getResource(THUMBS_UPLOAD_FORM), PATH_THUMBS, FILE_PART)
        );

        get(PATH_UPLOAD_META, (request, response) ->
                String.format(de.axxepta.converterservices.IOUtils.getResource(META_UPLOAD_FORM), PATH_META, FILE_PART)
        );

        get(PATH_UPLOAD_SPLIT, (request, response) ->
                String.format(de.axxepta.converterservices.IOUtils.getResource(PDFSPLIT_UPLOAD_FORM), PATH_SPLIT, FILE_PART, TO_IMAGE)
        );

        post(PATH_UPLOAD, MULTIPART_FORM_DATA, (request, response) -> {
            MultipartConfigElement multipartConfigElement = new MultipartConfigElement(STATIC_FILE_PATH);
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

        post(PATH_META, (request, response) -> {
            List<String> files = parseMultipartRequest(request, FILE_PART, new ArrayList<>());
            try {
                if (files.size() > 0) {
                    ByteArrayOutputStream out =
                            runExternal("exiftool", "-e", "-json", TEMP_FILE_PATH + "/" + files.get(0));
                    cleanTemp(files);
                    return new String(out.toByteArray(), "UTF-8");
                } else return NO_FILES_JSON;
            } catch (Exception e) {
                e.printStackTrace();
                return EXCEPTION_OPEN_JSON + e.getMessage() + EXCEPTION_CLOSE_JSON;
            }
        });

        post(PATH_SPLIT, MULTIPART_FORM_DATA, (request, response) -> {
            List<String> files;
            List<String> outputFiles;
            boolean toImage;
            try {
                List<String> partNames = new ArrayList<>();
                files = parseMultipartRequest(request, FILE_PART, partNames);
                toImage = partNames.contains(TO_IMAGE);
                outputFiles = PDFUtils.splitPDF(files.get(0), toImage, TEMP_FILE_PATH);
            } catch (IOException | InterruptedException ex) {
                return HTML_OPEN + ex.getMessage() + HTML_CLOSE;
            }

            if (outputFiles.size() > 0) {
                try {
                    HttpServletResponse raw = multiPartResponse(response);
                    for (String fileName : outputFiles) {
                        File file = new File(TEMP_FILE_PATH + "/" + fileName);
                        if (file.exists()) {
                            files.add(fileName);
                            try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
                                addMultiPartFile(raw.getOutputStream(), toImage ? TYPE_PNG : TYPE_PDF, is, fileName);
                            }
                        }
                    }
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

        get(PATH_STOP, (request, response) -> {
            stop();
            return "Services stopped.";
        });

    }

//########################################################

    private static List<String> thumbifyFiles(Request request) throws IOException, InterruptedException {
        List<String> files = parseMultipartRequest(request, FILE_PART, new ArrayList<>());
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
        de.axxepta.converterservices.IOUtils.copyStreams(is, os);
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

    /**
     * Extract names of all request parts, store contained files to disk
     * @param request request of type multipart
     * @param filePart name of parts from which files shall be extracted
     * @param partNames will contain the names of all request parts
     * @return list of filenames of files contained in parts named like filePart
     */
    private static List<String> parseMultipartRequest(Request request, String filePart, List<String> partNames) {
        MultipartConfigElement multipartConfigElement = new MultipartConfigElement(TEMP_FILE_PATH);
        request.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);

        File upload = new File(TEMP_FILE_PATH);
        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setRepository(upload);
        ServletFileUpload fileUpload = new ServletFileUpload(factory);
        try {
            List<FileItem> items = fileUpload.parseRequest(request.raw());
            items.forEach(item -> partNames.add(item.getFieldName()));
            return storeFilesTemporary(items, filePart);
        } catch (FileUploadException fu) {
            if (logger != null) logger.error(fu.getMessage());
            return new ArrayList<>();
        }
    }

    private static List<String> storeFilesTemporary(List<FileItem> items, String filePart) {
        List<String> files = new ArrayList<>();
        items.stream()
                .filter(e -> e.getFieldName().equals(filePart))
                .forEach(item -> {
                    try {
                        String fileName = item.getName();
                        FileItemHeaders headers = item.getHeaders();
                        if ((headers.getHeader(CONTENT_TRANSFER_ENCODING) != null)
                                && headers.getHeader(CONTENT_TRANSFER_ENCODING).toLowerCase().equals("base64")) {
                            InputStream is = item.getInputStream();
                            byte[] bytesFromBase64 = org.apache.commons.codec.binary.Base64.decodeBase64(IOUtils.toByteArray(is));
                            try (FileOutputStream os = new FileOutputStream(TEMP_FILE_PATH + "/" + fileName)) {
                                os.write(bytesFromBase64);
                            }
                        } else {
                            item.write(new File(TEMP_FILE_PATH, fileName));
                        }
                        files.add(fileName);
                    } catch (Exception ex) {
                        if (logger != null) logger.error(ex.getMessage());
                    }
                });
        return files;
    }

    private static void cleanTemp(List<String> files) {
        for (String file : files) {
            try {
                Files.delete(Paths.get(TEMP_FILE_PATH + "/" + file));
            } catch (IOException ex) {
                if (logger != null) logger.error(ex.getMessage());
            }
        }
    }

    private static ByteArrayOutputStream runExternal(String... command) throws IOException, InterruptedException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[1024];
            Process process = Runtime.getRuntime().exec(command);
            try (InputStream is = process.getInputStream()) {
                int n;
                while ((n = is.read(buffer)) > -1) {
                    stream.write(buffer, 0, n);
                }
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (logger != null) logger.error(line);
                }
            }
            int code = process.waitFor();
            if (logger != null) logger.debug("External program returned with code " + code);
        } catch (IOException | InterruptedException err) {
            if (logger != null) logger.error(err.getMessage());
            throw err;
        }
        return stream;
    }

}
