package de.axxepta.converterservices.servlet;

import de.axxepta.converterservices.proc.PipeExec;
import de.axxepta.converterservices.utils.IOUtils;
import spark.Request;
import spark.Response;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PipelineHandler extends RequestHandler {

    public PipelineHandler(Request request, Response response, String path, Map<String, String> formFields, Map<String, List<String>> files) {
        super(request, response, path, formFields, files);
    }

    public PipelineHandler(Request request, Response response, String path) {
        super(request, response, path);
    }

    @Override
    public Object processMulti() throws Exception {
        List<String> submittedFiles = this.files.getOrDefault(ServletUtils.FILE_PART, new ArrayList<>());
        String pipelineString = IOUtils.loadStringFromFile(IOUtils.pathCombine(path, submittedFiles.get(0)));

        return PipelineHandler.processPipeline(response, pipelineString, path);
    }


    private static Object processPipeline(Response response, String pipelineString, String tempPath) throws Exception {
        Object result = PipeExec.execProcessString(pipelineString, tempPath);
        if (result instanceof Integer && result.equals(-1)) {
            response.status(500);
            return ServletUtils.wrapResponse("Error during pipeline execution");
        } else {
            List<String> results = new ArrayList<>();
            if (result instanceof String) {
                results.add((String) result);
            }
            if (result instanceof List && ((List) result).get(0) instanceof String) {
                results.addAll( (List<String>) result );
            }
            if (results.size() == 1) {
                if (IOUtils.isFile(results.get(0))) {
                    return ServletUtils.buildSingleFileResponse(response, results.get(0));
                } else {
                    return ServletUtils.wrapResponse(result.toString());
                }
            } else if (results.size() > 1) {
                if (IOUtils.isFile(results.get(0))) {
                    HttpServletResponse raw = ServletUtils.multiPartResponse(response);
                    for (String fileName : results) {
                        if (IOUtils.isFile(fileName)) {
                            try (InputStream is = new BufferedInputStream(new FileInputStream(fileName))) {
                                String outputType = IOUtils.contentTypeByFileName(fileName);
                                ServletUtils.addMultiPartFile(raw.getOutputStream(), outputType, is, fileName);
                            }
                        }
                    }
                    ServletUtils.multiPartClose(raw.getOutputStream());
                    return raw;
                } else {
                    StringBuilder responseBuilder = new StringBuilder(ServletUtils.HTML_OPEN);
                    for (String fileName : results) {
                        responseBuilder = responseBuilder.append("<div>").append(fileName).append("</div>");
                    }
                    return responseBuilder.append(ServletUtils.HTML_CLOSE).toString();
                }
            } else {
                return ServletUtils.wrapResponse(result.toString());
            }
        }
    }

    public Object processSingle(boolean async) throws Exception {
        String pipelineString = request.body();
        if (async) {
            new Thread(() -> {
                try {
                    PipeExec.execProcessString(pipelineString, path);
                } catch (Exception ex) {
                    LOGGER.error("Error running pipeline in thread: ", ex);
                }
            }).start();
            return "<start>Pipeline started.</start>";
        } else {
            return PipelineHandler.processPipeline(response, pipelineString, path);
        }
    }
}
