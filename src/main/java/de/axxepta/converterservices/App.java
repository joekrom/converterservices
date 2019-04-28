package de.axxepta.converterservices;

import de.axxepta.converterservices.security.BasicAuthenticationFilter;
import de.axxepta.converterservices.security.SSLProvider;
import de.axxepta.converterservices.servlet.MailHandler;
import de.axxepta.converterservices.servlet.RequestHandler;
import de.axxepta.converterservices.servlet.PipelineHandler;
import de.axxepta.converterservices.servlet.ServletUtils;
import de.axxepta.converterservices.tools.CmdUtils;
import de.axxepta.converterservices.tools.ExcelUtils;
import de.axxepta.converterservices.tools.ImageUtils;
import de.axxepta.converterservices.tools.PDFUtils;
import de.axxepta.converterservices.utils.IOUtils;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.*;
import java.lang.reflect.Constructor;
import java.util.*;

import static de.axxepta.converterservices.servlet.ServletUtils.*;
import static spark.Spark.*;

public class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    public static final String STATIC_FILE_PATH = System.getProperty("user.home") + "/.converterservices";
    public static final String TEMP_FILE_PATH = STATIC_FILE_PATH + "/temp";

    private static final String PATH_HELLO              = "/hello";
    private static final String PATH_STOP               = "/stop";
    private static final String PATH_THUMB              = "/image/singlethumbnail";
    private static final String PATH_THUMBS             = "/image/thumbnail";
    private static final String PATH_META               = "/file/metadata";
    private static final String PATH_SPLIT              = "/pdf/split";
    private static final String PATH_EXCEL              = "/excel";
    private static final String PATH_SEND_MAIL          = "/send-mail";
    private static final String PATH_PIPELINE           = "/pipeline";
    private static final String PATH_PIPELINE_ASYNC     = "/pipeline-async";
    private static final String PATH_PIPELINE_MULTI     = "/pipeline-multi";
    private static final String PATH_DOWNLOAD           = "/static";
    private static final String PATH_UPLOAD             = "/upload";
    private static final String PATH_UPLOAD_THUMB       = "/form/thumb";
    private static final String PATH_UPLOAD_THUMBS      = "/form/thumbs";
    private static final String PATH_UPLOAD_META        = "/form/meta";
    private static final String PATH_UPLOAD_SPLIT       = "/form/pdfsplit";
    private static final String PATH_UPLOAD_EXCEL       = "/form/excel";
    private static final String PATH_UPLOAD_MAIL        = "/form/send-mail";
    private static final String PATH_UPLOAD_PIPELINE    = "/form/pipeline";

    private static final String HELLO_PAGE              = "static/hello.html";
    private static final String THUMB_UPLOAD_FORM       = "static/form_thumb.html";
    private static final String THUMBS_UPLOAD_FORM      = "static/form_thumbs.html";
    private static final String META_UPLOAD_FORM        = "static/form_meta.html";
    private static final String PDFSPLIT_UPLOAD_FORM    = "static/form_pdfsplit.html";
    private static final String EXCEL_UPLOAD_FORM       = "static/form_excel.html";
    private static final String MAIL_UPLOAD_FORM        = "static/form_sendmail.html";
    private static final String PIPELINE_UPLOAD_FORM    = "static/form_pipeline.html";

    private static final String AS_PNG                  = "AS_PNG";
    private static final String MULTIPART_FORM_DATA     = "multipart/form-data";
    private static final String NO_SUCH_FILE            = "<html><body><h1>File does not exist.</h1></body></html>";
    private static final String NO_FILES_JSON           = "{\"ERROR\": \"No temporary file found.\"}";
    private static final String EXCEPTION_OPEN_JSON     = "{\"EXCEPTION\": \"";
    private static final String EXCEPTION_CLOSE_JSON    = "\"}";

    private static String pwd = "";


    public static void main(String[] args) {

        if (args.length > 0)
            pwd = args[0];

        init("");

    }

//########################################################

    static void init(String basePath) {

        ServletUtils.setJettyLogLevel("WARN");

        SSLProvider.checkSSL();

        try {
            de.axxepta.converterservices.utils.IOUtils.safeCreateDirectory(STATIC_FILE_PATH);
            de.axxepta.converterservices.utils.IOUtils.safeCreateDirectory(TEMP_FILE_PATH);
        } catch (IOException ie) {
            LOGGER.error("Couldn't create directory for temporary files!");
        }

        before(new BasicAuthenticationFilter("/*"));

        get(basePath + PATH_HELLO, (request, response) ->
                String.format(de.axxepta.converterservices.utils.IOUtils.getResourceAsString(HELLO_PAGE),
                        basePath + PATH_THUMB, basePath + PATH_THUMBS,
                        basePath + PATH_META, basePath + PATH_SPLIT, basePath + PATH_UPLOAD_THUMB,
                        basePath + PATH_UPLOAD_THUMBS, basePath + PATH_UPLOAD_META, basePath + PATH_UPLOAD_SPLIT,
                        basePath + PATH_UPLOAD_EXCEL, basePath + PATH_UPLOAD_MAIL, basePath + PATH_UPLOAD_PIPELINE,
                        basePath + PATH_STOP)
        );

        get(basePath + PATH_DOWNLOAD + "/" + PARAM_NAME, (request, response) -> {
            HttpServletResponse raw = singleFileResponse(response, request.params(PARAM_NAME));
            File file = new File(STATIC_FILE_PATH + "/" + request.params(PARAM_NAME));
            if (file.exists()) {
                try (InputStream is = new FileInputStream(file)) {
                    de.axxepta.converterservices.utils.IOUtils.copyStreams(is, raw.getOutputStream());
                    raw.getOutputStream().close();
                } catch (Exception e) {
                    return wrapResponse(e.getMessage());
                }
            } else {
                return NO_SUCH_FILE;
            }
            return raw;
        });

        get(basePath + PATH_UPLOAD_THUMB, (request, response) ->
                String.format(de.axxepta.converterservices.utils.IOUtils.getResourceAsString(THUMB_UPLOAD_FORM),
                        basePath + PATH_THUMB, FILE_PART)
        );

        get(basePath + PATH_UPLOAD_META, (request, response) ->
                String.format(de.axxepta.converterservices.utils.IOUtils.getResourceAsString(META_UPLOAD_FORM),
                        basePath + PATH_META, PARAM_COMPACT, FILE_PART)
        );

        get(basePath + PATH_UPLOAD_SPLIT, (request, response) ->
                String.format(de.axxepta.converterservices.utils.IOUtils.getResourceAsString(PDFSPLIT_UPLOAD_FORM),
                        PARAM_VAL_PNG, PARAM_VAL_PDF, basePath + PATH_SPLIT, PARAM_AS, FILE_PART, AS_PNG)
        );

        get(basePath + PATH_UPLOAD_EXCEL, (request, response) ->
                String.format(de.axxepta.converterservices.utils.IOUtils.getResourceAsString(EXCEL_UPLOAD_FORM),
                        basePath + PATH_EXCEL, PARAM_AS, PARAM_CUSTOM_XML, PARAM_INDENT, PARAM_FIRST_ROW_NAME, PARAM_FIRST_COL_ID,
                        PARAM_COLUMN_FIRST, PARAM_SHEET_TAG, PARAM_ROW_TAG, PARAM_COLUMN_TAG, PARAM_SHEET_NAME,
                        PARAM_ATT_SHEET_NAME, PARAM_SEPARATOR, FILE_PART)
        );

        get(basePath + PATH_UPLOAD_MAIL, (request, response) ->
                String.format(de.axxepta.converterservices.utils.IOUtils.getResourceAsString(MAIL_UPLOAD_FORM),
                        basePath + PATH_SEND_MAIL,
                        PARAM_HOST, PARAM_PORT, PARAM_SECURE, PARAM_USER, PARAM_PWD, PARAM_SENDER, PARAM_RECEIVER, PARAM_SUBJECT,
                        PARAM_HTML, PARAM_IMAGES, PARAM_BASE, PARAM_ATTACHMENTS, FILE_PART, PARAM_CONTENT)
        );

        get(basePath + PATH_UPLOAD_PIPELINE, (request, response) ->
                String.format(de.axxepta.converterservices.utils.IOUtils.getResourceAsString(PIPELINE_UPLOAD_FORM),
                        basePath + PATH_PIPELINE_MULTI, FILE_PART)
        );


        post(basePath + PATH_THUMB + "/*/*", MULTIPART_FORM_DATA, App::singleImageHandling);

        post(basePath + PATH_THUMB + "/*", MULTIPART_FORM_DATA, App::singleImageHandling);

        post(basePath + PATH_THUMBS + "/*/*", MULTIPART_FORM_DATA, App::imageHandling);

        post(basePath + PATH_THUMBS + "/*", MULTIPART_FORM_DATA, App::imageHandling);

        post(basePath + PATH_UPLOAD, MULTIPART_FORM_DATA, (request, response) -> {
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

        post(basePath + PATH_META, (request, response) -> {
            boolean compact = checkQueryParameter(request, PARAM_COMPACT, false, "true", true);
            List<String> files =
                    parseMultipartRequest(request, FILE_PART, new HashMap<>(), TEMP_FILE_PATH).
                    getOrDefault(FILE_PART, new ArrayList<>());
            try {
                if (files.size() > 0) {
                    try (ByteArrayOutputStream out = CmdUtils.exif(compact, "-json", TEMP_FILE_PATH + "/" + files.get(0))) {
                        Core.cleanTemp(files);
                        return new String(out.toByteArray(), "UTF-8");
                    }
                } else return NO_FILES_JSON;
            } catch (Exception e) {
                e.printStackTrace();
                return EXCEPTION_OPEN_JSON + e.getMessage() + EXCEPTION_CLOSE_JSON;
            }
        });

        post(basePath + PATH_SPLIT, MULTIPART_FORM_DATA, (request, response) -> {
            boolean as_png = checkQueryParameter(request, PARAM_AS, false, PARAM_VAL_PNG, false);
            List<String> files;
            List<String> outputFiles;
            try {
                Map<String, String> formFields = new HashMap<>();
                files = parseMultipartRequest(request, FILE_PART, formFields, TEMP_FILE_PATH).
                        getOrDefault(FILE_PART, new ArrayList<>());
                /*if (partNames.contains(AS_PNG))
                    as_png = true;*/
                outputFiles = PDFUtils.splitPDF(files.get(0), as_png, TEMP_FILE_PATH);
            } catch (IOException | InterruptedException ex) {
                return wrapResponse(ex.getMessage());
            }

            if (outputFiles.size() > 0) {
                try {
                    HttpServletResponse raw = multiPartResponse(response);
                    for (String fileName : outputFiles) {
                        File file = new File(TEMP_FILE_PATH + "/" + fileName);
                        if (file.exists()) {
                            files.add(fileName);
                            try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
                                addMultiPartFile(raw.getOutputStream(), as_png ? Const.TYPE_PNG : Const.TYPE_PDF, is, fileName);
                            }
                        }
                    }
                    multiPartClose(raw.getOutputStream());
                    return raw;
                } catch (IOException ex) {
                    return wrapResponse(ex.getMessage());
                } finally {
                    Core.cleanTemp(files);
                }
            } else {
                return NO_SUCH_FILE;
            }
        });

        post(basePath + PATH_EXCEL, App::excelHandling);

        post(basePath + PATH_SEND_MAIL, MULTIPART_FORM_DATA, (request, response) -> handleMultipart(request, response, MailHandler.class));

        post(basePath + PATH_PIPELINE, Const.TYPE_XML, (request, response) -> handleSingle(request, response, false, PipelineHandler.class) );

        post(basePath + PATH_PIPELINE_ASYNC, Const.TYPE_XML, (request, response) -> handleSingle(request, response, true, PipelineHandler.class) );

        post(basePath + PATH_PIPELINE_MULTI, MULTIPART_FORM_DATA, (request, response) -> handleMultipart(request, response, PipelineHandler.class));

        get(basePath + PATH_STOP, (request, response) -> {
            if (getQueryParameter(request, PARAM_PWD).equals(pwd)) {
                stop();
                return "Services stopped.";
            } else {
                response.status(400);
                return "Wrong or no password provided for stopping server.";
            }

        });

    }


    private static Object handleMultipart(Request request, Response response, Class<? extends RequestHandler> clazz) {
        boolean cleanup = true;
        String dateString = Core.setTempPath();
        String tempPath = IOUtils.pathCombine(App.TEMP_FILE_PATH, dateString);
        try {
            if (!ServletFileUpload.isMultipartContent(request.raw())) {
                response.status(400);
                return wrapResponse("No multipart request");
            }

            Map<String, String> formFields = new HashMap<>();
            Map<String, List<String>> files =
                    parseMultipartRequest(request, "", formFields, tempPath);

            Map<String, String> parameters = request.params();
            if (parameters.containsKey(PARAM_CLEANUP)) {
                if (parameters.get(PARAM_CLEANUP).equals("false"))
                    cleanup = false;
            }

            Constructor<?> constructor = clazz.getConstructor(Request.class, Response.class, String.class, Map.class, Map.class);
            RequestHandler object = (RequestHandler) constructor.newInstance(request, response, tempPath, formFields, files);
            return object.processMulti();
        } catch (Exception ex) {
            response.status(500);
            return wrapResponse(ex.getMessage());
        } finally {
            if (cleanup) {
                Core.cleanup(dateString);
            } else {
                Core.releaseTemporaryDir(dateString);
            }
        }
    }

    private static Object handleSingle(Request request, Response response, boolean async, Class<? extends RequestHandler> clazz) {
        boolean cleanup = true;
        String dateString = Core.setTempPath();
        String tempPath = IOUtils.pathCombine(App.TEMP_FILE_PATH, dateString);
        try {
            Map<String, String> parameters = request.params();
            if (parameters.containsKey(PARAM_CLEANUP)) {
                if (parameters.get(PARAM_CLEANUP).equals("false"))
                    cleanup = false;
            }

            Constructor<?> constructor = clazz.getConstructor(Request.class, Response.class, String.class);
            RequestHandler object = (RequestHandler) constructor.newInstance(request, response, tempPath);
            return object.processSingle(async);
        } catch (Exception ex) {
            response.status(500);
            return wrapResponse(ex.getMessage());
        } finally {
            if (cleanup) {
                Core.cleanup(dateString);
            } else {
                Core.releaseTemporaryDir(dateString);
            }
        }
    }


    private static Object excelHandling(Request request, Response response) {
        String sheetName = getQueryParameter(request, PARAM_SHEET_NAME, "");
        String attSheetName = getQueryParameter(request, PARAM_ATT_SHEET_NAME, "");
        String as = getQueryParameter(request, PARAM_AS);
        String responseType = getQueryParameter(request, PARAM_RESPONSE, PARAM_VAL_MULTI);
        boolean customXMLMapping = checkQueryParameter(request, PARAM_CUSTOM_XML, false, "true", false);
        boolean indent = !checkQueryParameter(request, PARAM_INDENT, false, "false", false);
        boolean firstRowName = !checkQueryParameter(request, PARAM_FIRST_ROW_NAME, false, "false", false);
        boolean firstColumnId = checkQueryParameter(request, PARAM_FIRST_COL_ID, false, "true", false);
        boolean columnFirst = checkQueryParameter(request, PARAM_COLUMN_FIRST, false, "true", false);
        String sheet = getQueryParameter(request, PARAM_SHEET_TAG, ExcelUtils.SHEET_EL);
        String row = getQueryParameter(request, PARAM_ROW_TAG, ExcelUtils.ROW_EL);
        String column = getQueryParameter(request, PARAM_COLUMN_TAG, ExcelUtils.COL_EL);
        String separator = getQueryParameter(request, PARAM_SEPARATOR, ExcelUtils.DEF_SEPARATOR);

        List<String> files =
                parseMultipartRequest(request, FILE_PART, new HashMap<>(), TEMP_FILE_PATH).
                getOrDefault(FILE_PART, new ArrayList<>());
        List<String> convertedFiles = new ArrayList<>();
        for (String file : files) {
            if (de.axxepta.converterservices.utils.IOUtils.isXLSX(file)) {
                convertedFiles.addAll(ExcelUtils.fromTempExcel(file,
                        as.toLowerCase().equals("xml") ? ExcelUtils.FileType.XML : ExcelUtils.FileType.CSV,
                        customXMLMapping, sheetName, separator, indent, columnFirst,
                        firstRowName, firstColumnId, "", sheet, row, column, attSheetName));
            }
            if (de.axxepta.converterservices.utils.IOUtils.isCSV(file)) {
                convertedFiles.add(ExcelUtils.CSVToExcel(file,
                        (sheetName.equals("")) ? ExcelUtils.DEF_SHEET_NAME : sheetName, separator));
            }
            if (de.axxepta.converterservices.utils.IOUtils.isXML(file)) {
                convertedFiles.add(ExcelUtils.serviceXMLToExcel(file, sheetName, row, column, ExcelUtils.XML_SEPARATOR));
            }
        }

        if (files.size() > 0) {
            try {
                HttpServletResponse raw = responseType.toLowerCase().equals(PARAM_VAL_SINGLE) ?
                        singleFileResponse(response, convertedFiles.get(0)) :
                        multiPartResponse(response);
                int fileCounter = 0;
                for (String fileName : convertedFiles) {
                    File file = new File(TEMP_FILE_PATH + "/" + fileName);
                    if (file.exists()) {
                        files.add(fileName);
                        if (responseType.toLowerCase().equals(PARAM_VAL_SINGLE)) {
                            if (fileCounter == 0) {
                                try (InputStream is = new FileInputStream(file)) {
                                    de.axxepta.converterservices.utils.IOUtils.copyStreams(is, raw.getOutputStream());
                                    raw.getOutputStream().close();
                                }
                            }
                        } else {
                            String outputType;
                            if (de.axxepta.converterservices.utils.IOUtils.isXLSX(fileName))
                                outputType = Const.TYPE_XLSX;
                            else if (de.axxepta.converterservices.utils.IOUtils.isXML(fileName))
                                outputType = Const.TYPE_XML;
                            else
                                outputType = Const.TYPE_CSV;
                            try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
                                addMultiPartFile(raw.getOutputStream(), outputType, is, fileName);
                            }
                        }
                        fileCounter++;
                    }
                }
                if (responseType.toLowerCase().equals(PARAM_VAL_MULTI.toLowerCase()))
                    multiPartClose(raw.getOutputStream());
                return raw;
            } catch (IOException ex) {
                return wrapResponse(ex.getMessage());
            } finally {
                Core.cleanTemp(files);
            }
        } else {
            return NO_SUCH_FILE;
        }
    }


    private static Object singleImageHandling(Request request, Response response) {
        List<String> files;
        try {
            files = thumbifyFiles(request);
        } catch (IOException | InterruptedException ex) {
            return wrapResponse(ex.getMessage());
        }

        if (files.size() > 0) {
            HttpServletResponse raw = singleFileResponse(response, "thumb_" + ImageUtils.jpgFilename(files.get(0)));
            File file = new File(TEMP_FILE_PATH + "/" + ImageUtils.jpgFilename(files.get(0)));
            if (file.exists()) {
                files.add(ImageUtils.jpgFilename(files.get(0)));
                try (InputStream is = new FileInputStream(file)) {
                    de.axxepta.converterservices.utils.IOUtils.copyStreams(is, raw.getOutputStream());
                    raw.getOutputStream().close();
                    return raw;
                } catch (Exception e) {
                    return wrapResponse(e.getMessage());
                } finally {
                    Core.cleanTemp(files);
                }
            } else {
                Core.cleanTemp(files);
                return NO_SUCH_FILE;
            }
        } else {
            return NO_SUCH_FILE;
        }
    }

    private static Object imageHandling(Request request, Response response) {
        List<String> files;
        try {
            files = thumbifyFiles(request);
        } catch (IOException | InterruptedException ex) {
            return wrapResponse(ex.getMessage());
        }

        if (files.size() > 0) {
            try {
                List<String> transformedFiles = new ArrayList<>();
                HttpServletResponse raw = multiPartResponse(response);
                for (String fileName : files) {
                    File file = new File(TEMP_FILE_PATH + "/" + ImageUtils.jpgFilename(fileName));
                    if (file.exists()) {
                        transformedFiles.add(ImageUtils.jpgFilename(fileName));
                        try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
                            addMultiPartFile(raw.getOutputStream(), Const.TYPE_JPEG, is, ImageUtils.jpgFilename(fileName));
                        }
                    }
                }
                files.addAll(transformedFiles);
                multiPartClose(raw.getOutputStream());
                return raw;
            } catch (IOException ex) {
                return wrapResponse(ex.getMessage());
            } finally {
                Core.cleanTemp(files);
            }
        } else {
            return NO_SUCH_FILE;
        }
    }

    private static List<String> thumbifyFiles(Request request)
            throws IOException, InterruptedException {
        List<String> files =
                parseMultipartRequest(request, FILE_PART, new HashMap<>(), TEMP_FILE_PATH).
                getOrDefault(FILE_PART, new ArrayList<>());
        String size = request.splat()[0];
        String fit = "";
        if (request.splat().length > 1)
            fit = request.splat()[1];
        String scaling;
        switch (fit.toLowerCase()) {
            case PARAM_VAL_CROP: {
                scaling = size + "^"; break;    // scale to shorter side, eventually cropped
            }
            case PARAM_VAL_SCALE: {
                scaling = size + "!"; break;    // scale to values, aspect ratio ignored
            }
            default: scaling = size;            // scale to fit, eventually borders
        }
        try {
            for (String file : files) {
                ImageUtils.thumbify(scaling, size, TEMP_FILE_PATH + "/" + file);
            }
        } catch (IOException | InterruptedException ex) {
            Core.cleanTemp(files);
            throw ex;
        }
        return files;
    }

}
