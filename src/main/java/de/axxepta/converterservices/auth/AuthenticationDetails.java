package de.axxepta.converterservices.auth;

/**
 * Adapted from qmetric/spark-authentication
 */
public class AuthenticationDetails {

    private String username;
    private char[] password;

    String getUsername() {
        return username;
    }

    char[] getPassword() {
        return password;
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
