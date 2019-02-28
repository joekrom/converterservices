package de.axxepta.converterservices.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class RequestHandler {

    static final Logger LOGGER = LoggerFactory.getLogger(RequestHandler.class);

    Request request;
    Response response;
    String path;
    Map<String, String> formFields;
    Map<String, List<String>> files;

    RequestHandler(final Request request, final Response response, final String path,
                   final Map<String, String> formFields, final Map<String, List<String>> files) {
        this.request = request;
        this.response = response;
        this.path = path;
        this.formFields = formFields;
        this.files = files;
    }

    RequestHandler(final Request request, final Response response, final String path) {
        this.request = request;
        this.response = response;
        this.path = path;
        this.formFields = new HashMap<>();
        this.files = new HashMap<>();
    }

    public Object processMulti() throws Exception {
        response.status(400);
        return "Method not implemented";
    };

    public Object processSingle(boolean async) throws Exception {
        response.status(400);
        return "Method not implemented";
    };
}
