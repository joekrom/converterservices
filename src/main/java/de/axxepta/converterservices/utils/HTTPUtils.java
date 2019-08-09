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
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HTTPUtils {

    public static String getString(String protocol, String host, int port, String path, String user, String password,
                                 int timeout, String... accept) throws IOException
    {
        try (CloseableHttpClient httpClient = getClient(host, port, user, password, timeout)) {
            HttpGet httpget = new HttpGet(protocol + "://" + host + ":" + port + path);
            if (accept.length > 0) {
                httpget.setHeader("Accept", accept[0]);
            }
            try (CloseableHttpResponse response = httpClient.execute(httpget)) {
                return EntityUtils.toString(response.getEntity());
            }
        }
    }

    public static List<String> get(String protocol, String host, int port, String path, String user, String password,
                                   int timeout, String file, String... accept) throws IOException
    {
        List<String> responseFile = new ArrayList<>();
        //ToDo: Multipart
        try (CloseableHttpClient httpClient = getClient(host, port, user, password, timeout)) {
            HttpGet httpget = new HttpGet(protocol + "://" + host + ":" + port + path);
            if (accept.length > 0) {
                httpget.setHeader("Accept", accept[0]);
            }
            try (CloseableHttpResponse response = httpClient.execute(httpget)) {
                HttpEntity entity = response.getEntity();
                IOUtils.byteArrayToFile(EntityUtils.toByteArray(entity), file);
                responseFile.add(file);
                return responseFile;
            }
        }
    }

    public static List<String> get(String protocol, String host, int port, String path, String user, String password,
                                   int timeout, String file, boolean gullible, Map<String, String> headers) throws IOException
    {
        List<String> responseFile = new ArrayList<>();
        //ToDo: Multipart
        try (CloseableHttpClient httpClient = getClient(host, port, user, password, timeout, gullible)) {
            HttpGet httpget = new HttpGet(protocol + "://" + host + ":" + port + path);
            for (String key : headers.keySet()) {
                httpget.setHeader(key, headers.get(key));
            }
            try (CloseableHttpResponse response = httpClient.execute(httpget)) {
                HttpEntity entity = response.getEntity();
                IOUtils.byteArrayToFile(EntityUtils.toByteArray(entity), file);
                responseFile.add(file);
                return responseFile;
            }
        }
    }

    public static String getJSON(String protocol, String host, int port, String path, String user, String password, int timeout)
            throws IOException
    {
        return getString(protocol, host, port, path, user, password, timeout, "application/json");
    }

    public static String getXML(String protocol, String host, int port, String path, String user, String password, int timeout)
            throws IOException
    {
        return getString(protocol, host, port, path, user, password, timeout, "application/xml");
    }

    public static String getXmlFromJSON(String protocol, String host, int port, String path, String user, String password, int timeout)
            throws IOException, JSONException
    {
        String str = getJSON(protocol, host, port, path, user, password, timeout);
        return "<response>" + JSONUtils.JsonToXmlString(str) + "</response>";
    }


    public static String postXmlToJson(String protocol, String host, int port, String path, String user, String password,
                                       int timeout, String content) throws IOException
    {
        String jsonString = JSONUtils.XmlToJsonString(content);
        return postJSON(protocol, host, port, path, user, password, timeout, jsonString);
    }


    public static String post(String protocol, String host, int port, String path, String user, String password,
                                  int timeout, String content, ContentType... contentType) throws IOException {
        try (CloseableHttpClient httpClient = getClient(host, port, user, password, timeout)) {
            HttpPost httpPost = new HttpPost(protocol + "://" + host + ":" + port + path);
            HttpEntity stringEntity;
            if (contentType.length > 0) {
                stringEntity = new StringEntity(content, contentType[0]);
            } else {
                stringEntity = new StringEntity(content);
            }
            httpPost.setEntity(stringEntity);
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                return EntityUtils.toString(response.getEntity());
            }
        }
    }

    public static String postJSON(String protocol, String host, int port, String path, String user, String password,
                                  int timeout, String content) throws IOException
    {
        return post(protocol, host, port, path, user, password, timeout, content, ContentType.APPLICATION_JSON);
    }

    public static String postJSONFile(String protocol, String host, int port, String path, String user, String password,
                                  int timeout, String content) throws IOException
    {
        return post(protocol, host, port, path, user, password, timeout, content, ContentType.APPLICATION_JSON);
    }

    public static String postXML(String protocol, String host, int port, String path, String user, String password,
                                  int timeout, String file) throws IOException
    {
        String content = IOUtils.loadStringFromFile(file);
        return post(protocol, host, port, path, user, password, timeout, content, ContentType.APPLICATION_XML);
    }

    public static String postXMLFile(String protocol, String host, int port, String path, String user, String password,
                                 int timeout, String file) throws IOException
    {
        String content = IOUtils.loadStringFromFile(file);
        return post(protocol, host, port, path, user, password, timeout, content, ContentType.APPLICATION_XML);
    }

    public static String postTextTypeFile(String protocol, String host, int port, String path, String user, String password,
                                     int timeout, String file, ContentType contentType) throws IOException
    {
        String content = IOUtils.loadStringFromFile(file);
        return post(protocol, host, port, path, user, password, timeout, content, contentType);
    }

    public static String putString(String protocol, String host, int port, String path, String user, String password,
                                   int timeout, String content, ContentType... contentType) throws IOException
    {
        try (CloseableHttpClient httpClient = getClient(host, port, user, password, timeout)) {
            HttpPut httpPut = new HttpPut(protocol + "://" + host + ":" + port + path);
            HttpEntity stringEntity;
            if (contentType.length > 0) {
                stringEntity = new StringEntity(content, contentType[0]);
            } else {
                stringEntity = new StringEntity(content);
            }
            httpPut.setEntity(stringEntity);
            try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
                return EntityUtils.toString(response.getEntity());
            }
        }
    }

    public static String putTextTypeFile(String protocol, String host, int port, String path, String user, String password,
                                          int timeout, String file, ContentType contentType) throws IOException
    {
        String content = IOUtils.loadStringFromFile(file);
        return putString(protocol, host, port, path, user, password, timeout, content, contentType);
    }

    public static String putJSON(String protocol, String host, int port, String path, String user, String password,
                                 int timeout, String content) throws IOException
    {
        return putString(protocol, host, port, path, user, password, timeout, content, ContentType.APPLICATION_JSON);
    }

    public static String putJSONFile(String protocol, String host, int port, String path, String user, String password,
                                 int timeout, String file) throws IOException
    {
        String content = IOUtils.loadStringFromFile(file);
        return putString(protocol, host, port, path, user, password, timeout, content, ContentType.APPLICATION_JSON);
    }

    public static String putXML(String protocol, String host, int port, String path, String user, String password,
                                 int timeout, String content) throws IOException
    {
        return putString(protocol, host, port, path, user, password, timeout, content, ContentType.APPLICATION_XML);
    }

    public static String putXMLFile(String protocol, String host, int port, String path, String user, String password,
                                int timeout, String file) throws IOException
    {
        String content = IOUtils.loadStringFromFile(file);
        return putString(protocol, host, port, path, user, password, timeout, content, ContentType.APPLICATION_XML);
    }

    public static boolean contentTypeIsTextType(String contentType) {
        return contentType.toLowerCase().contains("xml") ||
                contentType.toLowerCase().contains("javascript") ||
                contentType.toLowerCase().contains("text") ||
                contentType.toLowerCase().contains("json");
    }

    public static boolean fileTypeIsTextType(String fileType) {
        return fileType.toLowerCase().equals("xml") ||
                fileType.toLowerCase().equals("js") ||
                fileType.toLowerCase().equals("txt") ||
                fileType.toLowerCase().equals("csv") ||
                fileType.toLowerCase().equals("css") ||
                fileType.toLowerCase().equals("rdf") ||
                fileType.toLowerCase().equals("md5") ||
                fileType.toLowerCase().equals("json");
    }

    private static CloseableHttpClient getClient(String host, int port, String user, String password, int timeout, boolean... gullible) {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000)
                .setSocketTimeout(timeout * 1000).build();
        HttpClientBuilder builder = HttpClients.custom()
                .setDefaultRequestConfig(config)
                //HACK:
                // true if it's OK to retry non-idempotent requests that have been sent
                // and then fail with network issues (not HTTP failures).
                //
                // "true" here will retry POST requests which have been sent but where
                // the response was not received. This arguably is a bit risky.
                .setRetryHandler(new DefaultHttpRequestRetryHandler(3, true));

        // configure client to ignore server's certificate chain, use only for https
        if (gullible.length > 0 && gullible[0]) {
            try {
                SSLContextBuilder contextBuilder = SSLContexts.custom();
                SSLContext sslContext = contextBuilder.
                        loadTrustMaterial(null, (X509Certificate[] chain, String authType) -> true).build();

                HostnameVerifier hostnameVerifier = NoopHostnameVerifier.INSTANCE;
                SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);

                Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
                        .<ConnectionSocketFactory>create().register("https", sslsf)
                        .build();

                PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(
                        socketFactoryRegistry);
                builder = builder.setConnectionManager(cm);
            } catch (Exception ex) {
                System.out.println("Ignoring overcredulous connection try...");
                ex.printStackTrace();
            }
        }

        if (!user.equals("")) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    new AuthScope(host, port),
                    new UsernamePasswordCredentials(user, password));

            builder = builder.setDefaultCredentialsProvider(credentialsProvider);
        }
        return builder.build();
    }

}
