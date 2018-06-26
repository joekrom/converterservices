package de.axxepta.converterservices.utils;

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
}
