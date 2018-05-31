package de.axxepta.converterservices.utils;

public class StringUtils {

    public static String[] nlListToArray(String nlList) {
        return nlList.split("\\r?\\n");
    }

    public static boolean isEmpty(Object s) {
        return (s == null) || !(s instanceof String) ||(s.equals(""));
    }
}
