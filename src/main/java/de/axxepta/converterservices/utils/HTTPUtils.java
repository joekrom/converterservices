package de.axxepta.converterservices.utils;

import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class HTTPUtils {

    public static String getJSON(String protocol, String host, int port, String path, String user, String password,
                                 String... accept) throws IOException
    {
        try (CloseableHttpClient httpClient = getClient(host, port, user, password)) {
            HttpGet httpget = new HttpGet(protocol + "://" + host + ":" + Integer.toString(port) + path);
            if (accept.length > 0) {
                httpget.setHeader("Accept", accept[0]);
            }
            try (CloseableHttpResponse response = httpClient.execute(httpget))
            {
                return EntityUtils.toString(response.getEntity());
            }
        }
    }

    public static String getJSON(String protocol, String host, int port, String path, String user, String password)
            throws IOException
    {
        return getJSON(protocol, host, port, path, user, password, "application/json");
    }

    public static String getXML(String protocol, String host, int port, String path, String user, String password)
            throws IOException
    {
        return getJSON(protocol, host, port, path, user, password, "application/xml");
    }

    public static String getXmlFromJSON(String protocol, String host, int port, String path, String user, String password)
            throws IOException
    {
        String str = getJSON(protocol, host, port, path, user, password);
        return "<response>" + JSONUtils.JsonToXmlString(str) + "</response>";
    }


    public static String postXmlToJson(String protocol, String host, int port, String path, String user, String password,
                                       String content) throws IOException
    {
        String jsonString = JSONUtils.XmlToJsonString(content);
        return postJSON(protocol, host, port, path, user, password, jsonString);
    }


    public static String post(String protocol, String host, int port, String path, String user, String password,
                                  String content, ContentType... contentType) throws IOException {
        try (CloseableHttpClient httpClient = getClient(host, port, user, password)) {
            HttpPost httpPost = new HttpPost(protocol + "://" + host + ":" + Integer.toString(port) + path);
            HttpEntity stringEntity;
            if (contentType.length > 0) {
                stringEntity = new StringEntity(content, contentType[0]);
            } else {
                stringEntity = new StringEntity(content);
            }
            httpPost.setEntity(stringEntity);
            try (CloseableHttpResponse response = httpClient.execute(httpPost))
            {
                return EntityUtils.toString(response.getEntity());
            }
        }
    }

    public static String postJSON(String protocol, String host, int port, String path, String user, String password,
                                  String content) throws IOException
    {
        return post(protocol, host, port, path, user, password, content, ContentType.APPLICATION_JSON);
    }


    public static String postXML(String protocol, String host, int port, String path, String user, String password,
                                  String content) throws IOException
    {
        return post(protocol, host, port, path, user, password, content, ContentType.APPLICATION_XML);
    }

    public static String put(String protocol, String host, int port, String path, String user, String password,
                             String content, ContentType... contentType) throws IOException
    {
        try (CloseableHttpClient httpClient = getClient(host, port, user, password)) {
            HttpPut httpPut = new HttpPut(protocol + "://" + host + ":" + Integer.toString(port) + path);
            HttpEntity stringEntity;
            if (contentType.length > 0) {
                stringEntity = new StringEntity(content, contentType[0]);
            } else {
                stringEntity = new StringEntity(content);
            }
            httpPut.setEntity(stringEntity);
            try (CloseableHttpResponse response = httpClient.execute(httpPut))
            {
                return EntityUtils.toString(response.getEntity());
            }
        }
    }

    public static String putJSON(String protocol, String host, int port, String path, String user, String password,
                                 String content) throws IOException
    {
        return put(protocol, host, port, path, user, password, content, ContentType.APPLICATION_JSON);
    }

    public static String putXML(String protocol, String host, int port, String path, String user, String password,
                                 String content) throws IOException
    {
        return put(protocol, host, port, path, user, password, content, ContentType.APPLICATION_XML);
    }

    private static CloseableHttpClient getClient(String host, int port, String user, String password) {
        int timeout = 600;
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000)
                .setSocketTimeout(timeout * 1000).build();
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                new AuthScope(host, port),
                new UsernamePasswordCredentials(user, password));
        return HttpClients.custom()
                .setDefaultCredentialsProvider(credentialsProvider)
                .setDefaultRequestConfig(config)

                //HACK:
                // true if it's OK to retry non-idempotent requests that have been sent
                // and then fail with network issues (not HTTP failures).
                //
                // "true" here will retry POST requests which have been sent but where
                // the response was not received. This arguably is a bit risky.
                .setRetryHandler(new DefaultHttpRequestRetryHandler(3, true))
                .build();
    }
}
