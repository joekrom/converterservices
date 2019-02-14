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
import java.util.*;

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

    /**
     * Get parameters encoded in request body
     * @param request Request
     * @param key Parameter name
     * @param defaultVal Optional default value to be returned if the parameter is not present
     * @return Value of parameter or provided default value
     */
    public static String getParameter(Request request, String key, String ... defaultVal) /*throws ServletException, IOException*/ {
        Map<String, String> params = request.params();
        return params.getOrDefault(key, defaultVal.length > 0 ? defaultVal[0] : "");
    }

    /**
     * Check for presence of parameters encoded in request body
     * @param request Request
     * @param key Parameter name
     * @param defaultVal Default value to be returned if the parameter is not present
     * @return Presence of parameter default value (false, if not provided)
     */
    public static boolean checkParameter(Request request, String key, boolean ... defaultVal) {
        Map<String, String> params = request.params();
        return params.containsKey(key) || defaultVal.length > 0 && defaultVal[0];
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
        IOUtils.copyStreams(is, os);
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
     * Extract names/values of all request parts (file and form fields), store contained files to disk
     * @param request request of type multipart
     * @param filePart name of parts from which files shall be extracted. If empty string, all file parts will be extracted
     * @param formFields will contain the key-value pairs of all form field request parts
     * @param storePath optional path for storage (standard is the temporary path)
     * @return map of file parts and associated lists of file names,
     *          if parameter filePart is non-empty String only of part with this name
     */
    public static Map<String, List<String>> parseMultipartRequest(Request request, String filePart, Map<String, String> formFields,
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
            items.forEach(
                    item ->  { if (item.isFormField()) formFields.put(item.getFieldName(), item.getString()); }
            );
            return processMultiParts(items, filePart, formFields, storagePath);
        } catch (FileUploadException fu) {
            if (LOGGER != null) LOGGER.error(fu.getMessage());
            return new HashMap<>();
        }
    }

    private static Map<String, List<String>> processMultiParts(List<FileItem> items, String filePart,
                                                               Map<String, String> formFields, String path)
    {
        Map<String, List<String>> files = new HashMap<>();
        items.forEach(item -> {
            if (item.isFormField()) {
                formFields.put(item.getFieldName(), item.getString());
            } else if (filePart.equals("") || item.getFieldName().equals(filePart)) {
                try {
                    String fieldName = item.getFieldName();
                    String fileName = item.getName();
                    FileItemHeaders headers = item.getHeaders();
                    if ((headers.getHeader(CONTENT_TRANSFER_ENCODING) != null)
                            && headers.getHeader(CONTENT_TRANSFER_ENCODING).toLowerCase().equals("base64")) {
                        InputStream is = item.getInputStream();
                        byte[] bytesFromBase64 =
                                org.apache.commons.codec.binary.Base64.decodeBase64(org.apache.commons.io.IOUtils.toByteArray(is));
                        try (FileOutputStream os = new FileOutputStream(path + "/" + fileName)) {
                            os.write(bytesFromBase64);
                        }
                    } else {
                        item.write(new File(path, fileName));
                    }
                    files.putIfAbsent(fieldName, new ArrayList<>());
                    files.get(fieldName).add(fileName);
                } catch (Exception ex) {
                    if (LOGGER != null) LOGGER.error(ex.getMessage());
                }
            }
        });
        return files;
    }
}
