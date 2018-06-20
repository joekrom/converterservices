package de.axxepta.converterservices.auth;

import spark.FilterImpl;
import spark.Request;
import spark.Response;
import spark.utils.SparkUtils;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static spark.Spark.halt;


/**
 * Adapted from qmetric/spark-authentication
 *
 * Define Spark Before-filter like
 * before(new BasicAuthenticationFilter("/path/*", new AuthenticationDetails("username", "password")));
 * in the main function
 */
public class BasicAuthenticationFilter extends FilterImpl {

    private static final String BASIC_AUTHENTICATION_TYPE = "Basic";
    private static final String ACCEPT_ALL_TYPES = "*";

    private final List<AuthenticationDetails> authenticationDetailList = new ArrayList<>();


    public BasicAuthenticationFilter() {
        super(SparkUtils.ALL_PATHS, ACCEPT_ALL_TYPES);
        authenticationDetailList.addAll(CredentialsProvider.getCredentials());
    }

    public BasicAuthenticationFilter(final String path) {
        super(path, ACCEPT_ALL_TYPES);
        authenticationDetailList.addAll(CredentialsProvider.getCredentials());
    }

    public BasicAuthenticationFilter(final AuthenticationDetails authenticationDetails) {
        this(SparkUtils.ALL_PATHS, authenticationDetails);
    }

    public BasicAuthenticationFilter(final String path, final AuthenticationDetails authenticationDetails) {
        super(path, ACCEPT_ALL_TYPES);
        this.authenticationDetailList.add(authenticationDetails);
    }

    @Override
    public void handle(final Request request, final Response response) {
        final String authString = request.headers("Authorization");

        boolean authenticated;
        if (authString == null) {
            authenticated = false;
        } else {
            String encodedHeader = request.headers("Authorization");
            int pos = encodedHeader.indexOf(BASIC_AUTHENTICATION_TYPE);
            authenticated = pos != -1 &&
                    authenticatedWith(credentialsFrom(encodedHeader.substring(pos + BASIC_AUTHENTICATION_TYPE.length() + 1)));
        }
        if (!authenticated) {
            response.header("WWW-Authenticate", BASIC_AUTHENTICATION_TYPE);
            halt(401);
        }
    }

    private String[] credentialsFrom(final String encodedHeader) {
        if (encodedHeader == null) {
            return new String[] { "", ""};
        }
        String decoded = decodeHeader(encodedHeader);
        int pos = decoded.indexOf(":");
        if (pos == -1) {
            return new String[] { decoded, ""};
        }
        return new String[] { decoded.substring(0, pos), decoded.substring(pos + 1) };
    }

    private String decodeHeader(final String encodedHeader) {
        return new String(Base64.getMimeDecoder().decode(encodedHeader));
    }

    private boolean authenticatedWith(final String[] credentials) {
        return authenticationDetailList.stream().
                anyMatch(a -> a.getUsername().equals(credentials[0]) && a.passwordEquals(credentials[1]));
    }

}
