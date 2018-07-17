package de.axxepta.converterservices.utils;

import de.axxepta.converterservices.App;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemHeaders;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ServletUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServletUtils.class);

    private static final String MULTI_PART_BOUNDARY     = "MULTI_PART_BOUNDARY";
    private static final String CONTENT_TRANSFER_ENCODING     = "Content-Transfer-Encoding";


    public static String getQueryParameter(Request request, String key, String ... defaultVal) {
        Set<String> params = request.queryParams();
        if (params.contains(key))
            return request.queryParams(key);
        else {
            if (defaultVal.length > 0)
                return defaultVal[0];
            else
                return "";
        }
    }

    public static boolean checkQueryParameter(Request request, String key, boolean caseSens, String value, boolean defaultVal) {
        String val = getQueryParameter(request, key);
        if (val.equals("")) {
            return defaultVal;
        } else {
            if (caseSens) {
                return val.equals(value);
            } else {
                return val.toLowerCase().equals(value.toLowerCase());
            }
        }
    }


    public static Object buildSingleFileResponse(Response response, String fileName) throws IOException {
        HttpServletResponse raw = ServletUtils.singleFileResponse(response, IOUtils.filenameFromPath(fileName));
        File file = new File(fileName);
        try (InputStream is = new FileInputStream(file)) {
            IOUtils.copyStreams(is, raw.getOutputStream());
            raw.getOutputStream().close();
            return raw;
        }
    }

    public static HttpServletResponse singleFileResponse(Response response, String fileName) {
        HttpServletResponse raw = response.raw();
        response.header("Content-Disposition", "attachment; filename=" + fileName);
        response.type("application/force-download");
        return raw;
    }

    public static HttpServletResponse multiPartResponse(Response response) throws IOException {
        HttpServletResponse raw = response.raw();
        response.type("multipart/x-mixed-replace;boundary=" + MULTI_PART_BOUNDARY);
        ServletOutputStream os = raw.getOutputStream();
        os.println();
        os.println("--" + MULTI_PART_BOUNDARY);
        return raw;
    }

    public static void addMultiPartFile(ServletOutputStream os, String contentType, InputStream is, String name) throws IOException {
        os.println("--" + MULTI_PART_BOUNDARY);
        os.println("Content-Disposition: attachment; filename=\"" + name + "\"");
        os.println("Content-Type: " + contentType);
        os.println();
        de.axxepta.converterservices.utils.IOUtils.copyStreams(is, os);
        os.println("--" + MULTI_PART_BOUNDARY);     // ?
        os.flush();
    }

    public static void multiPartClose(ServletOutputStream os) throws IOException {
        os.println();
        os.flush();
        os.println("--" + MULTI_PART_BOUNDARY + "--");
        os.close();
    }

    /**
     * Extract names of all request parts, store contained files to disk
     * @param request request of type multipart
     * @param filePart name of parts from which files shall be extracted
     * @param partNames will contain the names of all request parts
     * @param storePath optional path for storage (standard is the temporary path)
     * @return list of file names of files contained in parts named like filePart
     */
    public static List<String> parseMultipartRequest(Request request, String filePart, List<String> partNames,
                                                     String... storePath)
    {
        String storagePath = storePath.length > 0 ? storePath[0] : App.TEMP_FILE_PATH;
        MultipartConfigElement multipartConfigElement = new MultipartConfigElement(storagePath);
        request.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);

        File upload = new File(storagePath);
        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setRepository(upload);
        ServletFileUpload fileUpload = new ServletFileUpload(factory);
        try {
            List<FileItem> items = fileUpload.parseRequest(request.raw());
            items.forEach(item -> partNames.add(item.getFieldName()));
            return storeFilesTemporary(items, filePart, storagePath);
        } catch (FileUploadException fu) {
            if (LOGGER != null) LOGGER.error(fu.getMessage());
            return new ArrayList<>();
        }
    }

    private static List<String> storeFilesTemporary(List<FileItem> items, String filePart, String path) {
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
                            byte[] bytesFromBase64 = org.apache.commons.codec.binary.Base64.decodeBase64(org.apache.commons.io.IOUtils.toByteArray(is));
                            try (FileOutputStream os = new FileOutputStream(path + "/" + fileName)) {
                                os.write(bytesFromBase64);
                            }
                        } else {
                            item.write(new File(path, fileName));
                        }
                        files.add(fileName);
                    } catch (Exception ex) {
                        if (LOGGER != null) LOGGER.error(ex.getMessage());
                    }
                });
        return files;
    }
}
