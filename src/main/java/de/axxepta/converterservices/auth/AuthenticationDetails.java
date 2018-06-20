package de.axxepta.converterservices.auth;

import java.util.Arrays;

/**
 * Adapted from qmetric/spark-authentication
 */
public class AuthenticationDetails {

    private final String username;
    private char[] password;

    String getUsername() {
        return username;
    }

    boolean passwordEquals(final String providedPwd) {
        char[] pwdChars = providedPwd.toCharArray();
        return pwdChars.length == password.length && Arrays.equals(pwdChars, password);
    }

    public AuthenticationDetails(final String username, final String password) {
        if (username == null) {
            this.username = "";
        } else {
            this.username = username;
        }
        try {
            this.password = password.toCharArray();
        } catch (NullPointerException ne) {
            this.password = new char[0];
        }
    }
}
