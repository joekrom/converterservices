package de.axxepta.converterservices.utils;

import java.util.Base64;

public class StringUtils {

    public static String[] nlListToArray(String nlList) {
        return nlList.split("\\r?\\n");
    }

    public static boolean isNoStringOrEmpty(Object s) {
        return (s == null) || !(s instanceof String) ||(s.equals(""));
    }

    public static boolean isEmpty(String s) {
        return (s == null) || (s.equals(""));
    }

    public static boolean isInt(String str)
    {
        try {
            int i = Integer.parseInt(str);
        } catch(NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    public static String decodeBase64(String input) {
        byte[] decoded = Base64.getDecoder().decode(input);
        return new String(decoded);
    }

    public static String decodeBase64(byte[] input) {
        byte[] decoded = Base64.getDecoder().decode(input);
        return new String(decoded);
    }

    public static String encodeBase64(String input) {
        byte[] encoded = Base64.getEncoder().encode(input.getBytes());
        return new String(encoded);
    }

    public static String encodeBase64(byte[] input) {
        byte[] encoded = Base64.getEncoder().encode(input);
        return new String(encoded);
    }

}
